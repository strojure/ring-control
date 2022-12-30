(ns strojure.ring-control.handler
  "Functions for building Ring handler from configuration."
  (:require [strojure.ring-control.config :as config]))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

;; TODO: Validate for duplicate wrappers

(defn- validate-required
  [{:keys [ignore-required] :as config}]
  (let [config-tags (-> (select-keys config [:outer :enter :leave :inner])
                        (update-vals (partial map config/type-tag)))
        ignore-required (map config/type-tag ignore-required)
        match-type-tag (fn [parent] (fn [child] (isa? child parent)))]
    (doseq [wrap-seq,,,,,,,,,,,,,,, (vals config)
            wrap,,,,,,,,,,,,,,,,,,, wrap-seq
            [config-group req-tags] (config/required wrap)
            req-tag,,,,,,,,,,,,,,,, req-tags
            :when (not (some (match-type-tag req-tag) ignore-required))]
      (when-not (->> (get config-tags config-group)
                     (take-while (complement (match-type-tag (config/type-tag wrap))))
                     (some (match-type-tag req-tag)))
        (throw (ex-info (str (if (some (match-type-tag req-tag) (get config-tags config-group))
                               ;; TODO: change position to order?
                               "Required in wrong position: "
                               "Missing required: ")
                             {config-group (config/type-tag wrap) :required req-tag})
                        {:type (config/type-tag wrap)
                         :required (config/required wrap)
                         :missing req-tag}))))))

(defn- apply-handler-wraps
  [handler fs]
  (->> (reverse fs)
       (map config/wrap-handler-fn)
       (reduce (fn [handler wrap-fn] (wrap-fn handler))
               handler)))

(defn- apply-request-wraps
  [handler fs]
  (let [request-fn (->> (reverse fs)
                        (map config/wrap-request-fn)
                        (reduce (fn [f wrap-fn]
                                  (fn [request]
                                    (f (wrap-fn request))))))]
    (fn
      ([request]
       (handler (request-fn request)))
      ([request respond raise]
       (handler (request-fn request) respond raise)))))

(defn- apply-response-wraps
  [handler fs]
  (let [response-fn (->> (reverse fs)
                         (map config/wrap-response-fn)
                         (reduce (fn [f wrap-fn]
                                   (fn [response request]
                                     (f (wrap-fn response request) request)))))]
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
  {:arglists '([handler {:keys [outer enter leave inner ignore-required]}])}
  [handler {:keys [outer enter leave inner] :as config}]
  (validate-required config)
  (cond-> handler
    (seq inner) (apply-handler-wraps inner)
    (seq leave) (apply-response-wraps leave)
    (seq enter) (apply-request-wraps enter)
    (seq outer) (apply-handler-wraps outer)))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
