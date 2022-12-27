(ns strojure.ring-stack.middleware.ring-core
  "Supplemental functions for the middlewares from the `ring.middleware`
  namespace."
  (:require [ring.middleware.content-type :as content-type]
            [ring.middleware.keyword-params :as keyword-params]
            [ring.middleware.params :as params]
            [strojure.ring-stack.core :as stack])
  (:import (clojure.lang MultiFn)))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn as-wrap-with-options
  "Returns function to use `wrapper-fn` as wrapper multimethod with optional
  `options` argument, which can accept maps and pass it as `option`."
  [wrapper-fn]
  (fn [this]
    (if (map? this) (wrapper-fn this)
                    (wrapper-fn))))

(defn request-wrap-fn
  "Returns wrapper to be used in `:enter`."
  [request-fn type-symbol]
  (fn request-wrapper*
    ([] (request-wrapper* {}))
    ([options]
     (-> (fn [request]
           (request-fn request options))
         (with-meta {:type type-symbol})))))

(defn response-wrap-fn
  "Returns wrapper to be used in `:leave`."
  [response-fn type-symbol]
  (fn response-wrapper*
    ([] (response-wrapper* {}))
    ([options]
     (-> (fn [response request]
           (response-fn response request options))
         (with-meta {:type type-symbol})))))

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
  (request-wrap-fn params/params-request `request-params))

(.addMethod ^MultiFn stack/as-request-wrap `request-params
            (as-wrap-with-options request-params))

(derive ::request-params `request-params)

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
  (request-wrap-fn keyword-params/keyword-params-request `request-keyword-params))

(.addMethod ^MultiFn stack/as-request-wrap `request-keyword-params
            (as-wrap-with-options request-keyword-params))

(derive ::request-keyword-params `request-keyword-params)

(defmethod stack/required-config `request-keyword-params [_] {:enter [`request-params]})

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
  (response-wrap-fn content-type/content-type-response `response-content-type))

(.addMethod ^MultiFn stack/as-response-wrap `response-content-type
            (as-wrap-with-options response-content-type))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(comment

  (defn test-config [config]
    (let [handler (-> (fn [request] {:request request})
                      (stack/wrap-handler config))]
      (handler {:uri "/" :query-string "a=1"})))

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
  )

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
