(ns strojure.ring-control.config
  "Builder configuration implementation."
  (:import (clojure.lang MultiFn Named)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn type-tag
  "Returns dispatch value for the object.

  - for keyword or symbol returns it
  - for objects with `:type` in meta returns `(type-tag (:type (meta obj)))`
  - for map with `:type` returns its `(type-tag (:type obj))`
  - for class returns the class
  - otherwise returns `(class obj)`
  "
  [obj]
  (or (when (instance? Named obj) obj)
      (some-> obj meta :type type-tag)
      (some-> obj :type type-tag)
      (when (class? obj) class)
      (class obj)))

(defn derive-type-tag
  "Establishes a parent/child relationship between parent tag and `tag`. Parent
  must be a namespace-qualified symbol or keyword."
  [parent tag]
  (derive (type-tag tag) parent))

(defn with-type-tag
  "Returns object associated with :type `tag`."
  [obj tag]
  (vary-meta obj assoc :type tag))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defmulti wrap-handler-fn
  "Returns Ring middleware `(fn [handler] new-handler)` for the object."
  {:arglists '([obj])}
  type-tag)

(defmulti wrap-request-fn
  "Returns function `(fn [request] new-request)` for the object."
  {:arglists '([obj])}
  type-tag)

(defmulti wrap-response-fn
  "Returns function `(fn [response request] new-response)` for the object."
  {:arglists '([obj])}
  type-tag)

(defmulti required
  "Returns configuration `{:keys [outer enter leave inner]}` where every key
  contains sequence of type tags to be presented in configuration before the
  current tag."
  {:arglists '([obj])}
  type-tag)

;; Nothing is required by default.
(.addMethod ^MultiFn required :default (constantly nil))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn set-required
  "Assigns [[required-config]] value for the type `tag`."
  [tag config]
  (.addMethod ^MultiFn required tag (constantly config)))

(defn- set-wrap-method
  {:arglists '([multi tag method]
               [multi tag method {:keys [tags requires]}])}
  ([multi tag method] (set-wrap-method multi method tag nil))
  ([multi tag method options]
   (.addMethod ^MultiFn multi tag method)
   ;; Derive more tags from the `tag`.
   (run! (partial derive-type-tag tag)
         (:tags options))
   ;; Set required config.
   (some->> (:requires options)
            (set-required tag))))

(def ^{:arglists '([tag f]
                   [tag f {:keys [tags requires]}])}
  as-wrap-handler
  "Associates function `(fn [_] (fn [handler] new-handler))` with type `tag`.

  Options:

  - `:tags`     – a sequence of type tags to derive from the `tag`.
  - `:requires` – a required configuration to validate for the `tag`.
  "
  (partial set-wrap-method wrap-handler-fn))

(def ^{:arglists '([tag f]
                   [tag f {:keys [tags requires]}])}
  as-wrap-request
  "Associates function `(fn [_] (fn [request] new-request)) with type `tag`.

  Options:

  - `:tags`     – a sequence of type tags to derive from the `tag`.
  - `:requires` – a required configuration to validate for the `tag`.
  "
  (partial set-wrap-method wrap-request-fn))

(def ^{:arglists '([tag f]
                   [tag f {:keys [tags requires]}])}
  as-wrap-response
  "Associates function `(fn [_] (fn [response request] new-response))` with type
  `tag`.

  Options:

  - `:tags`     – a sequence of type tags to derive from the `tag`.
  - `:requires` – a required configuration to validate for the `tag`.
  "
  (partial set-wrap-method wrap-response-fn))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
