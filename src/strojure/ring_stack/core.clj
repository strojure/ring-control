(ns strojure.ring-stack.core
  (:import (clojure.lang MultiFn Named)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn middleware-type
  "Returns dispatch value for middleware object.

  - for keyword or symbol returns it
  - for objects with `:type` in meta returns its value
  - for map with `:type` returns its value
  - otherwise returns `(class obj)`
  "
  [obj]
  (or (when (instance? Named obj) obj)
      (some-> obj meta :type)
      (:type obj)
      (class obj)))

(defmulti as-handler-wrap
  "Coerce object to Ring handler wrapper."
  {:arglists '([obj])}
  middleware-type)

(defmulti as-request-wrap
  "Coerce object to Ring request wrapper, the function
  `(fn [request] new-request)`."
  {:arglists '([obj])}
  middleware-type)

(defmulti as-response-wrap
  "Coerce object to Ring response wrapper, the function
  `(fn [response request] new-response)`."
  {:arglists '([obj])}
  middleware-type)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defmulti required-config
  "Returns configuration `{:keys [outer enter leave inner]}` where every key
  contains sequence of middleware types to be presented in configuration before
  the middleware."
  {:arglists '([obj])}
  middleware-type)

;; No middleware dependencies by default.
(.addMethod ^MultiFn required-config :default (constantly nil))

(defn- match-type?
  [parent]
  (fn [child] (isa? child parent)))

(defn- validate-required
  [{:keys [ignore-required] :as config}]
  (let [config-types (-> (select-keys config [:outer :enter :leave :inner])
                         (update-vals (partial map middleware-type)))
        ignore (set ignore-required)]
    (doseq [[_ group-middlewares], config
            middleware,,,,,,,,,,,, group-middlewares
            [config-key req-types] (required-config middleware)
            req-type,,,,,,,,,,,,,, req-types
            :when (not (ignore req-type))]
      (when-not (->> (config-key config-types)
                     (take-while (complement (match-type? (middleware-type middleware))))
                     (some (match-type? req-type)))
        (throw (ex-info (str (if (some (match-type? req-type) (config-key config-types))
                               "Required middleware is in wrong position: "
                               "Missing required middleware: ")
                             {:middleware (middleware-type middleware)
                              :requires req-type})
                        {:required-type (middleware-type middleware)
                         :required-config (required-config middleware)
                         :middleware middleware
                         :missing req-type}))))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn- apply-handler-wraps
  [handler fns]
  (->> (reverse fns)
       (map as-handler-wrap)
       (reduce (fn [handler wrapper] (wrapper handler))
               handler)))

(defn- apply-request-wraps
  [handler fns]
  (let [request-fn (->> (reverse fns)
                        (map as-request-wrap)
                        (reduce (fn [f ff] (fn [request]
                                             (f (ff request))))))]
    (fn
      ([request]
       (handler (request-fn request)))
      ([request respond raise]
       (handler (request-fn request) respond raise)))))

(defn- apply-response-wraps
  [handler fns]
  (let [response-fn (->> (reverse fns)
                         (map as-response-wrap)
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

(defn wrap-handler
  "Returns ring handler wrapped by middlewares in configuration map:

  - `:outer` Standard ring middlewares to wrap around all other wrappers.
  - `:enter` Ring request wrapping functions `(fn [request] new-request)`.
  - `:leave` Ring response wrapping functions `(fn [response request] new-response)`.
             The function receive same `request` as wrapping handler itself.
  - `:inner` Standard ring middlewares to wrap just around `handler` after
             `:enter` and before `:leave`.

  Wrapper are applying in direct order:

      ;; Call `(enter1 request)` before `(enter2 request)`.
      {:enter [enter1
               enter2]}

  Configuration groups are applied as they are listed above:

  - Request flow:
      - `:outer` -> `:enter` -> `:inner` -> handler.
  - Response flow:
      - handler -> `:inner` -> `:leave` -> `:outer`.

  Such configuration allows to distinguish between request/response handlers,
  control order of wrappers more easy and naturally comparing with usage of
  standard ring middlewares only.

  Wrapping functions can be defined with types using multimethods
  [[as-handler-wrap]], [[as-request-wrap]], [[as-response-wrap]] and be referred
  in configuration:

      {:enter [::enter1
               ::enter2
               {:type ::enter3 :opt1 true :opt2 false}]}

  Same type can be defined as request wrapper and as response wrapper. They
  should be specified in `:enter` and `:leave` independently.

  In this case we can also define dependency of `::enter2` on `::enter2` using
  [[require-config]] multimethod:

      (defmethod require-config ::enter2 [_]
        {:enter [::enter1]})

      ;; This fails with exception about missing middleware:
      (wrap-handler handler {:enter [::enter2]})

      ;; This fails with exception about wrong order:
      (wrap-handler handler {:enter [::enter2 ::enter1]})

      ;; But this succeeds anyway:
      (wrap-handler handler {:enter [::enter2]
                             :ignore-required [::enter1]})
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
