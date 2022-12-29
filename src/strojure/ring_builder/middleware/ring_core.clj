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
  (let [f (-> (fn [& {:as options}]
                (let [options (or options {})]
                  (-> (fn [request] (ring-request-fn request options))
                      (config/with-type-tag tag))))
              (config/with-type-tag tag))]
    (config/as-wrap-request tag (wrap-options f) as-request-opts)
    f))

(defn wrap-response-fn
  "Returns wrap method implementation for ring response function."
  [ring-response-fn, tag, as-response-opts]
  (let [f (-> (fn [& {:as options}]
                (let [options (or options {})]
                  (-> (fn [response request] (ring-response-fn response request options))
                      (config/with-type-tag tag))))
              (config/with-type-tag tag))]
    (config/as-wrap-response tag (wrap-options f) as-response-opts)
    f))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([& {:as options}])}
  params-request
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
                   `params-request {:tags [::params-request]}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([& {:as options}])}
  keyword-params-request
  "Middleware that converts any string keys in the :params map to keywords.
  Only keys that can be turned into valid keywords are converted.

  This middleware does not alter the maps under `:*-params` keys. These are left
  as strings.

  Accepts the following options:

  - `:parse-namespaces?` – if true, parse the parameters into namespaced
                           keywords (defaults to false)
  "
  (wrap-request-fn keyword-params/keyword-params-request
                   `keyword-params-request {:tags [::keyword-params-request]
                                            :requires {:enter [`params-request]}}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([& {:as options}])}
  content-type-response
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
                    `content-type-response {:tags [::content-type-response]}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn test-config [config]
  (let [handler (-> (fn [request] {:request request})
                    (handler/build config))]
    (handler {:uri "/" :query-string "a=1"})))

(comment
  (test-config {:enter [params-request]})
  (test-config {:enter [(params-request)]})
  (test-config {:enter [{:type `params-request :encoding "UTF-8"}]})
  (test-config {:enter [{:type ::params-request :encoding "UTF-8"}]})
  (test-config {:enter [(params-request)
                        (keyword-params-request)]
                :leave [(content-type-response)]})
  (test-config {:enter [{:type `params-request :encoding "UTF-8"}
                        {:type `keyword-params-request :parse-namespaces? true}]})
  (test-config {:enter [{:type ::params-request :encoding "UTF-8"}
                        {:type ::keyword-params-request :parse-namespaces? true}]})
  (config/type-tag params-request)
  (config/type-tag keyword-params-request)
  (isa? (config/type-tag params-request) `params-request)
  (test-config {:enter [{:type params-request :encoding "UTF-8"}
                        {:type keyword-params-request :parse-namespaces? true}]})
  )

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
