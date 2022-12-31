(ns strojure.ring-control.handler
  "Functions for building Ring handler from configuration."
  (:require [strojure.ring-control.config :as config]))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn- validate-duplicates
  [{{:keys [request response]} ::flow-types}]
  (letfn [(validate [group xs]
            (when-let [dubs (->> (frequencies xs)
                                 (keep (fn [[k v]] (when (< 1 v) k)))
                                 (seq))]
              (throw (ex-info (str "Duplicates in config: " dubs)
                              {:duplicates dubs group xs}))))]
    (validate :request request)
    (validate :response response)))

(defn- validate-required
  [{:keys [::flow-items ::flow-types ignore-required]}]
  (let [ignore-required (map config/type-tag ignore-required)
        tag-matcher (fn [parent] (fn [child] (isa? child parent)))]
    (doseq [[tag required] (->> (concat (:request flow-items)
                                        (:response flow-items))
                                (distinct)
                                (keep (fn [x]
                                        (when-let [required (config/required x)]
                                          [(config/type-tag x) required]))))
            [group req-seq] required
            req-tag,,,,,,,, req-seq
            :when (not (some (tag-matcher req-tag) ignore-required))]
      (when-not (->> (get flow-types group)
                     (take-while (complement (tag-matcher tag)))
                     (some (tag-matcher req-tag)))
        (throw (ex-info (str (if (some (tag-matcher req-tag) (get flow-types group))
                               "Misplaced required: " "Missing required: ")
                             {:type tag :required [group req-tag]})
                        {:type tag
                         :required required
                         :missing req-tag}))))))

(defn- apply-handler-wraps
  [handler fs]
  (->> (reverse fs)
       (map config/handler-fn)
       (reduce (fn [handler ff] (ff handler))
               handler)))

(defn- with-flow
  "Attaches request flow to `config` for validation purpose."
  [{:keys [outer enter leave inner] :as config}]
  (let [request-flow (into [] cat [outer enter inner])
        response-flow (into () cat [outer (reverse leave) inner])]
    (assoc config ::flow-items {:request request-flow
                                :response response-flow}
                  ::flow-types {:request (map config/type-tag request-flow)
                                :response (map config/type-tag response-flow)})))

(comment
  (with-flow {:outer ['o1 'o2 'o3]
              :enter ['e1 'e2 'e3]
              :leave ['l1 'l2 'l3]
              :inner ['i1 'i2 'i3]})
  )

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

      (config/set-required `enter2 {:request [`enter1]})

      ;; This fails with exception about missing required:
      (handler/build handler {:enter [`enter2]})

      ;; This fails with exception about wrong order:
      (handler/build handler {:enter [`enter2 `enter1]})

      ;; But this succeeds anyway:
      (handler/build handler {:enter [`enter2]
                              :ignore-required [`enter1]})
  "
  {:arglists '([handler {:as config :keys [outer enter leave inner ignore-required]}])}
  [handler {:keys [outer enter leave inner] :as config}]
  (doto (with-flow config)
    (validate-duplicates)
    (validate-required))
  (cond-> handler
    (seq inner) (apply-handler-wraps inner)
    (seq leave) (apply-response-wraps leave)
    (seq enter) (apply-request-wraps enter)
    (seq outer) (apply-handler-wraps outer)))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
