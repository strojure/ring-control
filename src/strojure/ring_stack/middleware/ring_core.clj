(ns strojure.ring-stack.middleware.ring-core
  "Supplemental functions for the middlewares from the `ring.middleware`
  namespace."
  (:require [ring.middleware.content-type :as content-type]
            [ring.middleware.keyword-params :as keyword-params]
            [ring.middleware.params :as params]
            [strojure.ring-stack.core :as stack]))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn as-with-options
  "Returns middleware method function to use `as-fn` as multimethod with
  optional `options` argument, which can accept maps and pass it as `option` in
  `as-fn`."
  [as-fn]
  (fn [obj]
    (if (map? obj) (as-fn obj)
                   (as-fn))))

(defn as-request-fn
  "Returns `(fn [request] new-request)` to be used in `:enter`."
  [ring-request-fn, type-symbol, set-as-request-opts]
  (let [f (-> (fn as-request-fn*
                ([] (as-request-fn* {}))
                ([options]
                 (-> (fn [request] (ring-request-fn request options))
                     (with-meta {:type type-symbol}))))
              (with-meta {:type type-symbol}))]
    (stack/set-as-request-fn type-symbol (as-with-options f) set-as-request-opts)
    f))

(defn as-response-fn
  "Returns `(fn [response request] new-response)` to be used in `:leave`."
  [ring-response-fn, type-symbol, set-as-response-opts]
  (let [f (-> (fn as-response-fn*
                ([] (as-response-fn* {}))
                ([options]
                 (-> (fn [response request] (ring-response-fn response request options))
                     (with-meta {:type type-symbol}))))
              (with-meta {:type type-symbol}))]
    (stack/set-as-response-fn type-symbol (as-with-options f) set-as-response-opts)
    f))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([] [options])}
  request-params
  "Middleware to parse urlencoded parameters from the query string and form
  body (if the request is a url-encoded form). Adds the following keys to
  the request map:

  - `:query-params` - a map of parameters from the query string
  - `:form-params`  - a map of parameters from the body
  - `:params`       - a merged map of all types of parameter

  Accepts the following options:

  - `:encoding` - encoding to use for url-decoding. If not specified, uses
                  the request character encoding, or \"UTF-8\" if no request
                  character encoding is set.
  "
  (as-request-fn params/params-request
                 `request-params {:type-aliases [::request-params]}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([] [options])}
  request-keyword-params
  "Middleware that converts any string keys in the :params map to keywords.
  Only keys that can be turned into valid keywords are converted.

  This middleware does not alter the maps under `:*-params` keys. These are left
  as strings.

  Accepts the following options:

  - `:parse-namespaces?` - if true, parse the parameters into namespaced
                           keywords (defaults to false)
  "
  (as-request-fn keyword-params/keyword-params-request
                 `request-keyword-params {:type-aliases [::request-keyword-params]
                                          :required-config {:enter [`request-params]}}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([] [options])}
  response-content-type
  "Middleware that adds a content-type header to the response if one is not
  set by the handler. Uses the `ring.util.mime-type/ext-mime-type` function to
  guess the content-type from the file extension in the URI. If no
  content-type can be found, it defaults to 'application/octet-stream'.

  Accepts the following options:

  - `:mime-types` - a map of filename extensions to mime-types that will be
                    used in addition to the ones defined in
                    `ring.util.mime-type/default-mime-types`
  "
  (as-response-fn content-type/content-type-response
                  `response-content-type {:type-aliases [::response-content-type]}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn test-config [config]
  (let [handler (-> (fn [request] {:request request})
                    (stack/wrap-handler config))]
    (handler {:uri "/" :query-string "a=1"})))

(comment
  (test-config {:enter [request-params]})
  (test-config {:enter [(request-params)]})
  (test-config {:enter [{:type `request-params :encoding "UTF-8"}]})
  (test-config {:enter [{:type ::request-params :encoding "UTF-8"}]})
  (test-config {:enter [(request-params)
                        (request-keyword-params)]
                :leave [(response-content-type)]})
  (test-config {:enter [{:type `request-params :encoding "UTF-8"}
                        {:type `request-keyword-params :parse-namespaces? true}]})
  (test-config {:enter [{:type ::request-params :encoding "UTF-8"}
                        {:type ::request-keyword-params :parse-namespaces? true}]})
  (stack/object-type request-params)
  (stack/object-type request-keyword-params)
  (isa? (stack/object-type request-params) `request-params)
  (test-config {:enter [{:type request-params :encoding "UTF-8"}
                        {:type request-keyword-params :parse-namespaces? true}]})
  )

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
