(ns strojure.ring-control.handler
  "Functions for building Ring handler from configuration."
  (:require [strojure.ring-control.config :as config]))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn- validate-duplicates
  [{:keys [outer enter leave inner] :as config-tags}]
  (letfn [(duplicates [xs] (->> (frequencies xs)
                                (keep (fn [[k v]] (when (< 1 v) k)))
                                (seq)))]
    (when-let [xs (duplicates (concat outer inner))]
      (throw (ex-info (str "Duplicate handler wraps: " xs)
                      {:duplicates xs :config (select-keys config-tags [:outer :inner])})))
    (when-let [xs (duplicates enter)]
      (throw (ex-info (str "Duplicate request wraps: " xs)
                      {:duplicates xs :config (select-keys config-tags [:enter])})))
    (when-let [xs (duplicates leave)]
      (throw (ex-info (str "Duplicate response wraps: " xs)
                      {:duplicates xs :config (select-keys config-tags [:leave])})))))

(defn- validate-required
  [config-tags, ignore-required]
  (let [ignore-required (map config/type-tag ignore-required)
        match-type-tag (fn [parent] (fn [child] (isa? child parent)))]
    (doseq [wrap-seq,,,,,,,,,,,,,,, (vals config-tags)
            wrap,,,,,,,,,,,,,,,,,,, wrap-seq
            [config-group req-tags] (config/required wrap)
            req-tag,,,,,,,,,,,,,,,, req-tags
            :when (not (some (match-type-tag req-tag) ignore-required))]
      (when-not (->> (get config-tags config-group)
                     (take-while (complement (match-type-tag wrap)))
                     (some (match-type-tag req-tag)))
        (throw (ex-info (str (if (some (match-type-tag req-tag) (get config-tags config-group))
                               "Required in wrong position: "
                               "Missing required: ")
                             {config-group wrap :required req-tag})
                        {:type wrap
                         :required (config/required wrap)
                         :missing req-tag}))))))

(defn- apply-handler-wraps
  [handler fs]
  (->> (reverse fs)
       (map config/handler-fn)
       (reduce (fn [handler ff] (ff handler))
               handler)))

(defn- apply-request-wraps
  [handler fs]
  (let [request-fn (->> (reverse fs)
                        (map config/request-fn)
                        (reduce (fn [f ff]
                                  (fn [request]
                                    (f (ff request))))))]
    (fn
      ([request]
       (handler (request-fn request)))
      ([request respond raise]
       (handler (request-fn request) respond raise)))))

(defn- apply-response-wraps
  [handler fs]
  (let [response-fn (->> (reverse fs)
                         (map config/response-fn)
                         (reduce (fn [f ff]
                                   (fn [response request]
                                     (f (ff response request) request)))))]
    (fn
      ([request]
       (response-fn (handler request) request))
      ([request respond raise]
       (handler request
                (fn [resp] (respond (response-fn resp request)))
                raise)))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn build
  "Returns ring handler applying configuration options:

  - `:outer` – A sequence of standard ring middlewares to wrap handler before
               all other wraps.
  - `:enter` – A sequence of Ring request functions `(fn [request] new-request)`.
  - `:leave` – A sequence of Ring response functions `(fn [response request]
               new-response)`. The function receives same `request` as `handler`.
  - `:inner` – A sequence of standard ring middlewares to wrap the `handler`
               after `:enter` and before `:leave` wraps.

  The wraps are applying in direct order:

      ;; Applies enter1 before enter2.
      {:enter [`enter1
               `enter2]}

  Configuration groups are applied as they are listed above:

  - Request flow:
      - `:outer` −> `:enter` −> `:inner` −> handler.
  - Response flow:
      - handler −> `:inner` −> `:leave` −> `:outer`.

  Such configuration allows to distinguish between request/response only wraps,
  control order of application more easy and naturally comparing with standard
  usage of ring middlewares.

  The wraps should be tagged with symbols (and optionally with convenient type
  tags) using [[config/as-wrap-handler]], [[config/as-wrap-request]],
  [[config/as-wrap-response]] to be referred in configuration:

      {:enter [`enter1
               `enter2
               {:type `enter3 :opt1 true :opt2 false}]}

  We can also define dependency of `` `enter2 `` on `` `enter1 `` using
  [[config/set-required]] function:

      (config/set-required `enter2 {:enter [`enter1]})

      ;; This fails with exception about missing required:
      (handler/build handler {:enter [`enter2]})

      ;; This fails with exception about wrong order:
      (handler/build handler {:enter [`enter2 `enter1]})

      ;; But this succeeds anyway:
      (handler/build handler {:enter [`enter2]
                              :ignore-required [`enter1]})
  "
  [handler {:keys [outer enter leave inner ignore-required] :as config}]
  (doto (-> (select-keys config [:outer :enter :leave :inner])
            (update-vals (partial map config/type-tag)))
    (validate-duplicates)
    (validate-required ignore-required))
  (cond-> handler
    (seq inner) (apply-handler-wraps inner)
    (seq leave) (apply-response-wraps leave)
    (seq enter) (apply-request-wraps enter)
    (seq outer) (apply-handler-wraps outer)))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
