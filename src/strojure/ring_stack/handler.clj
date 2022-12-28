(ns strojure.ring-stack.handler
  "Functions for building Ring handler from middleware configuration."
  (:require [strojure.ring-stack.middleware :as mid]))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

;; TODO: Validate for duplicate wrappers

(defn- validate-required
  [{:keys [ignore-required] :as config}]
  (let [config-types (-> (select-keys config [:outer :enter :leave :inner])
                         (update-vals (partial map mid/object-type)))
        ignore-required (map mid/object-type ignore-required)
        match-type (fn [parent] (fn [child] (isa? child parent)))]
    (doseq [[_ group-middlewares], config
            middleware,,,,,,,,,,,, group-middlewares
            [config-key req-types] (mid/required-config middleware)
            req-type,,,,,,,,,,,,,, req-types
            :when (not (some (match-type req-type) ignore-required))]
      (when-not (->> (config-key config-types)
                     (take-while (complement (match-type (mid/object-type middleware))))
                     (some (match-type req-type)))
        (throw (ex-info (str (if (some (match-type req-type) (config-key config-types))
                               "Required middleware in wrong position: "
                               "Missing required middleware: ")
                             {:middleware (mid/object-type middleware)
                              :requires req-type})
                        {:middleware (mid/object-type middleware)
                         :required-config (mid/required-config middleware)
                         :missing req-type}))))))

(defn- apply-handler-fs
  [handler fs]
  (->> (reverse fs)
       (map mid/as-handler-fn)
       (reduce (fn [handler wrap-fn] (wrap-fn handler))
               handler)))

(defn- apply-request-fs
  [handler fs]
  (let [request-fn (->> (reverse fs)
                        (map mid/as-request-fn)
                        (reduce (fn [f ff] (fn [request]
                                             (f (ff request))))))]
    (fn
      ([request]
       (handler (request-fn request)))
      ([request respond raise]
       (handler (request-fn request) respond raise)))))

(defn- apply-response-fs
  [handler fs]
  (let [response-fn (->> (reverse fs)
                         (map mid/as-response-fn)
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
  "Returns ring handler applying middlewares in configuration map:

  - `:outer` Standard ring middlewares to wrap around all other middlewares.
  - `:enter` Ring request functions `(fn [request] new-request)`.
  - `:leave` Ring response functions `(fn [response request] new-response)`.
             The function receive same `request` as `handler`.
  - `:inner` Standard ring middlewares to wrap just around `handler` after
             `:enter` and before `:leave` middlewares.

  Wrapper are applying in direct order:

      ;; Applies enter1 before enter2.
      {:enter [`enter1
               `enter2]}

  Configuration groups are applied as they are listed above:

  - Request flow:
      - `:outer` −> `:enter` −> `:inner` −> handler.
  - Response flow:
      - handler −> `:inner` −> `:leave` −> `:outer`.

  Such configuration allows to distinguish between request/response only
  middleware, control order of application more easy and naturally comparing
  with standard usage of ring middlewares.

  Middleware functions should be associated with symbols (and additional
  convenient type aliases) using [[middleware/set-handler-fn]],
  [[middleware/set-request-fn]], [[middleware/set-response-fn]] to be referred
  in configuration:

      {:enter [`enter1
               `enter2
               {:type `enter3 :opt1 true :opt2 false}]}

  Same type symbol can be used for request and response. It is used in `:enter`
  and `:leave` independently.

  We can also define dependency of `` `enter2 `` on `` `enter1 `` using
  [[middleware/set-require-config]] function:

      (middleware/set-require-config `enter2 {:enter [`enter1]})

      ;; This fails with exception about missing middleware:
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
    (seq inner) (apply-handler-fs inner)
    (seq leave) (apply-response-fs leave)
    (seq enter) (apply-request-fs enter)
    (seq outer) (apply-handler-fs outer)))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
