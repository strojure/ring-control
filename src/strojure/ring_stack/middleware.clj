(ns strojure.ring-stack.middleware
  "Middleware configuration implementation."
  (:import (clojure.lang MultiFn Named)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn object-type
  "Returns dispatch value for middleware object.

  - for keyword or symbol returns it
  - for objects with `:type` in meta returns `(object-type (:type (meta obj)))`
  - for map with `:type` returns its `(object-type (:type obj))`
  - for class returns the class
  - otherwise returns `(class obj)`
  "
  [obj]
  (or (when (instance? Named obj) obj)
      (some-> obj meta :type object-type)
      (some-> obj :type object-type)
      (when (class? obj) class)
      (class obj)))

(defn derive-type
  "Establishes a parent/child relationship between parent and object. Parent
  must be a namespace-qualified symbol or keyword."
  [parent object]
  (derive (object-type object) parent))

(defn with-type
  "Returns object associated with object type `t`."
  [obj t]
  (vary-meta obj assoc :type t))

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

(defn set-required-config
  "Assigns required config for type symbol."
  [type-sym config]
  (.addMethod ^MultiFn required-config type-sym (constantly config)))

(defn- set-method
  {:arglists '([mf type-sym method]
               [mf type-sym method {:keys [type-aliases required-config]}])}
  ([mf type-sym method] (set-method mf method type-sym nil))
  ([mf type-sym method options]
   (.addMethod ^MultiFn mf type-sym method)
   ;; Derive type aliases from the type.
   (run! (partial derive-type type-sym)
         (:type-aliases options))
   ;; Add method for `required-config`.
   (some->> (:required-config options)
            (set-required-config type-sym))))

(def ^{:arglists '([type-sym f]
                   [type-sym f {:keys [type-aliases required-config]}])}
  set-handler-fn
  "Associates function `f` (returning `(fn [handler] new-handler)`) with type
  symbol.

  Options:

  - `:type-aliases`    - the sequence of symbols to derive from `type-symbol`.
  - `:required-config` - the middleware configuration to validate for the type.
  "
  (partial set-method as-handler-fn))

(def ^{:arglists '([type-sym f]
                   [type-sym f {:keys [type-aliases required-config]}])}
  set-request-fn
  "Associates function `f` (returning `(fn [request] new-request)`) with type
  symbol.

  Options:

  - `:type-aliases`    - the sequence of symbols to derive from `type-symbol`.
  - `:required-config` - the middleware configuration to validate for the type.
  "
  (partial set-method as-request-fn))

(def ^{:arglists '([type-sym f]
                   [type-sym f {:keys [type-aliases required-config]}])}
  set-response-fn
  "Associates function `f` (returning `(fn [response request] new-response)`)
  with type symbol.

  Options:

  - `:type-aliases`    - the sequence of symbols to derive from `type-symbol`.
  - `:required-config` - the middleware configuration to validate for the type.
  "
  (partial set-method as-response-fn))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
