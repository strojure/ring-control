(ns strojure.ring-builder.middleware.ring-core
  "Builder configuration functions for the middlewares from the
  `ring.middleware` namespace."
  (:require [ring.middleware.content-type :as content-type]
            [ring.middleware.keyword-params :as keyword-params]
            [ring.middleware.params :as params]
            [strojure.ring-builder.config :as config]
            [strojure.ring-builder.handler :as handler]))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn wrap-options
  "Returns wrap method function to use `wrap-fn` as multimethod with optional
  `options` argument, which can accept maps and pass it as `option` in
  `wrap-fn`."
  [wrap-fn]
  (fn [obj]
    (if (map? obj) (wrap-fn obj)
                   (wrap-fn))))

(defn wrap-request-fn
  "Returns wrap method implementation for ring request function."
  [ring-request-fn, tag, as-request-opts]
  (let [f (-> (fn wrap-request-fn*
                ([] (wrap-request-fn* {}))
                ([options]
                 (-> (fn [request] (ring-request-fn request options))
                     (config/with-type-tag tag))))
              (config/with-type-tag tag))]
    (config/as-wrap-request tag (wrap-options f) as-request-opts)
    f))

(defn wrap-response-fn
  "Returns wrap method implementation for ring response function."
  [ring-response-fn, tag, as-response-opts]
  (let [f (-> (fn wrap-response-fn*
                ([] (wrap-response-fn* {}))
                ([options]
                 (-> (fn [response request] (ring-response-fn response request options))
                     (config/with-type-tag tag))))
              (config/with-type-tag tag))]
    (config/as-wrap-response tag (wrap-options f) as-response-opts)
    f))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([] [options])}
  request-params
  "Middleware to parse urlencoded parameters from the query string and form
  body (if the request is an url-encoded form). Adds the following keys to
  the request map:

  - `:query-params` – a map of parameters from the query string
  - `:form-params`  – a map of parameters from the body
  - `:params`       – a merged map of all types of parameter

  Accepts the following options:

  - `:encoding` – encoding to use for url-decoding. If not specified, uses
                  the request character encoding, or \"UTF-8\" if no request
                  character encoding is set.
  "
  (wrap-request-fn params/params-request
                   `request-params {:tags [::request-params]}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([] [options])}
  request-keyword-params
  "Middleware that converts any string keys in the :params map to keywords.
  Only keys that can be turned into valid keywords are converted.

  This middleware does not alter the maps under `:*-params` keys. These are left
  as strings.

  Accepts the following options:

  - `:parse-namespaces?` – if true, parse the parameters into namespaced
                           keywords (defaults to false)
  "
  (wrap-request-fn keyword-params/keyword-params-request
                   `request-keyword-params {:tags [::request-keyword-params]
                                            :requires {:enter [`request-params]}}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([] [options])}
  response-content-type
  "Middleware that adds a content-type header to the response if one is not
  set by the handler. Uses the `ring.util.mime-type/ext-mime-type` function to
  guess the content-type from the file extension in the URI. If no
  content-type can be found, it defaults to 'application/octet-stream'.

  Accepts the following options:

  - `:mime-types` – a map of filename extensions to mime-types that will be
                    used in addition to the ones defined in
                    `ring.util.mime-type/default-mime-types`
  "
  (wrap-response-fn content-type/content-type-response
                    `response-content-type {:tags [::response-content-type]}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn test-config [config]
  (let [handler (-> (fn [request] {:request request})
                    (handler/build config))]
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
  (config/type-tag request-params)
  (config/type-tag request-keyword-params)
  (isa? (config/type-tag request-params) `request-params)
  (test-config {:enter [{:type request-params :encoding "UTF-8"}
                        {:type request-keyword-params :parse-namespaces? true}]})
  )

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
