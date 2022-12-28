(ns strojure.ring-stack.core
  (:import (clojure.lang MultiFn Named)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn object-type
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

(defn derive-object-type
  "Establishes a parent/child relationship between parent and object. Parent
  must be a namespace-qualified symbol or keyword."
  [parent object]
  (derive (object-type object) parent))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defmulti as-handler-fn
  "Returns Ring middleware `(fn [handler] new-handler)` for the object."
  {:arglists '([obj])}
  object-type)

(defmulti as-request-fn
  "Returns function `(fn [request] new-request)` for the object."
  {:arglists '([obj])}
  object-type)

(defmulti as-response-fn
  "Returns function `(fn [response request] new-response)` for the object."
  {:arglists '([obj])}
  object-type)

(defmulti required-config
  "Returns configuration `{:keys [outer enter leave inner]}` where every key
  contains sequence of middleware types to be presented in configuration before
  the middleware."
  {:arglists '([obj])}
  object-type)

;; No middleware dependencies by default.
(.addMethod ^MultiFn required-config :default (constantly nil))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn- set-method
  {:arglists '([mf type-sym method]
               [mf type-sym method {:keys [type-aliases required-config]}])}
  ([mf type-sym method] (set-method mf method type-sym nil))
  ([mf type-sym method options]
   (.addMethod ^MultiFn mf type-sym method)
   ;; Derive type aliases from the type.
   (run! (partial derive-object-type type-sym)
         (:type-aliases options))
   ;; Add method for `required-config`.
   (some->> (:required-config options)
            (constantly)
            (.addMethod ^MultiFn required-config type-sym))))

(def ^{:arglists '([type-sym f]
                   [type-sym f {:keys [type-aliases required-config]}])}
  set-as-handler-fn
  "Associates function `f` (returning `(fn [handler] new-handler)`) with type
  symbol.

  Options:

  - `:type-aliases`    - the sequence of symbols to derive from `type-symbol`.
  - `:required-config` - the middleware configuration to validate for the type.
  "
  (partial set-method as-handler-fn))

(def ^{:arglists '([type-sym f]
                   [type-sym f {:keys [type-aliases required-config]}])}
  set-as-request-fn
  "Associates function `f` (returning `(fn [request] new-request)`) with type
  symbol.

  Options:

  - `:type-aliases`    - the sequence of symbols to derive from `type-symbol`.
  - `:required-config` - the middleware configuration to validate for the type.
  "
  (partial set-method as-request-fn))

(def ^{:arglists '([type-sym f]
                   [type-sym f {:keys [type-aliases required-config]}])}
  set-as-response-fn
  "Associates function `f` (returning `(fn [response request] new-response)`)
  with type symbol.

  Options:

  - `:type-aliases`    - the sequence of symbols to derive from `type-symbol`.
  - `:required-config` - the middleware configuration to validate for the type.
  "
  (partial set-method as-response-fn))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn- validate-required
  [{:keys [ignore-required] :as config}]
  (let [config-types (-> (select-keys config [:outer :enter :leave :inner])
                         (update-vals (partial map object-type)))
        ignore (set ignore-required)
        match-type (fn [parent] (fn [child] (isa? child parent)))]
    (doseq [[_ group-middlewares], config
            middleware,,,,,,,,,,,, group-middlewares
            [config-key req-types] (required-config middleware)
            req-type,,,,,,,,,,,,,, req-types
            :when (not (ignore req-type))]
      (when-not (->> (config-key config-types)
                     (take-while (complement (match-type (object-type middleware))))
                     (some (match-type req-type)))
        (throw (ex-info (str (if (some (match-type req-type) (config-key config-types))
                               "Required middleware is in wrong position: "
                               "Missing required middleware: ")
                             {:middleware (object-type middleware)
                              :requires req-type})
                        {:required-type (object-type middleware)
                         :required-config (required-config middleware)
                         :middleware middleware
                         :missing req-type}))))))

(defn- apply-handler-fs
  [handler fs]
  (->> (reverse fs)
       (map as-handler-fn)
       (reduce (fn [handler wrap-fn] (wrap-fn handler))
               handler)))

(defn- apply-request-fs
  [handler fs]
  (let [request-fn (->> (reverse fs)
                        (map as-request-fn)
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
                         (map as-response-fn)
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
      - `:outer` −> `:enter` −> `:inner` −> handler.
  - Response flow:
      - handler −> `:inner` −> `:leave` −> `:outer`.

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
    (seq inner) (apply-handler-fs inner)
    (seq leave) (apply-response-fs leave)
    (seq enter) (apply-request-fs enter)
    (seq outer) (apply-handler-fs outer)))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
