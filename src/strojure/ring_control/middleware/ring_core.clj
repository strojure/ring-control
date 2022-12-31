(ns strojure.ring-control.middleware.ring-core
  "Builder configuration functions for the middlewares from the
  `ring.middleware` namespace."
  (:require [ring.middleware.content-type :as content-type]
            [ring.middleware.cookies :as cookies]
            [ring.middleware.flash :as flash]
            [ring.middleware.keyword-params :as keyword-params]
            [ring.middleware.params :as params]
            [ring.middleware.session :as session]
            [strojure.ring-control.config :as config]))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn- tag-options-fn
  [tag]
  (-> (fn [& {:as options}]
        (-> (or options {})
            (config/with-type-tag tag)))
      (config/with-type-tag tag)))

(defn without-options
  "Marks ring function as having no `options` argument."
  [ring-fn]
  (vary-meta ring-fn assoc ::without-options true))

(defn- has-options?
  [ring-fn]
  (not (-> ring-fn meta ::without-options)))

(defn as-handler-fn
  "Returns wrap method implementation for ring handler middleware."
  [tag, ring-handler-fn, as-wrap-opts]
  (letfn [(wrap-fn [obj]
            (if (has-options? ring-handler-fn)
              (let [options (if (map? obj) obj {})]
                (fn wrap-handler [handler]
                  (ring-handler-fn handler options)))
              ring-handler-fn))]
    (config/as-handler-fn tag wrap-fn as-wrap-opts)
    (tag-options-fn tag)))

(defn as-request-fn
  "Returns wrap method implementation for ring request function."
  [tag, ring-request-fn, as-wrap-opts]
  (letfn [(wrap-fn [obj]
            (if (has-options? ring-request-fn)
              (let [options (if (map? obj) obj {})]
                (fn wrap-request [request]
                  (ring-request-fn request options)))
              ring-request-fn))]
    (config/as-request-fn tag wrap-fn as-wrap-opts)
    (tag-options-fn tag)))

(defn as-response-fn
  "Returns wrap method implementation for ring response function."
  [tag, ring-response-fn, as-wrap-opts]
  (letfn [(wrap-fn [obj]
            (if (has-options? ring-response-fn)
              (let [options (if (map? obj) obj {})]
                (fn wrap-response [response request]
                  (ring-response-fn response request options)))
              ring-response-fn))]
    (config/as-response-fn tag wrap-fn as-wrap-opts)
    (tag-options-fn tag)))

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
  (as-request-fn `params-request params/params-request
                 {:tags [::params-request]}))

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

  Requires [[params-request]].
  "
  (as-request-fn `keyword-params-request keyword-params/keyword-params-request
                 {:tags [::keyword-params-request]
                  :requires {:request [`params-request]}}))

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
  (as-response-fn `content-type-response content-type/content-type-response
                  {:tags [::content-type-response]}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([& {:as options}])}
  wrap-cookies
  "Parses the cookies in the request map, then assocs the resulting map
  to the :cookies key on the request.

  Accepts the following options:

  - `:decoder`
      + A function to decode the cookie value. Expects a function that takes a
        string and returns a string.
      + Defaults to URL-decoding.

  - `:encoder`
      + A function to encode the cookie name and value. Expects a function that
        takes a name/value map and returns a string.
      + Defaults to URL-encoding.

  Each cookie is represented as a map, with its value being held in the
  `:value` key. A cookie may optionally contain a `:path`, `:domain` or `:port`
  attribute.

  To set cookies, add a map to the :cookies key on the response. The values
  of the cookie map can either be strings, or maps containing the following
  keys:

  - `:value`     – the new value of the cookie
  - `:path`      – the sub-path the cookie is valid for
  - `:domain`    – the domain the cookie is valid for
  - `:max-age`   – the maximum age in seconds of the cookie
  - `:expires`   – a date string at which the cookie will expire
  - `:secure`    – set to true if the cookie requires HTTPS, prevent HTTP access
  - `:http-only` – set to true if the cookie is valid for HTTP and HTTPS only
                   (i.e. prevent JavaScript access)
  - `:same-site` – set to `:strict` or `:lax` to set SameSite attribute of the
                   cookie
  "
  (as-handler-fn `wrap-cookies cookies/wrap-cookies
                 {:tags [::wrap-cookies]}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([& {:as options}])}
  wrap-session
  "Reads in the current HTTP session map, and adds it to the `:session` key on
  the request. If a `:session` key is added to the response by the handler, the
  session is updated with the new value. If the value is nil, the session is
  deleted.

  Accepts the following options:

  - `:store`
      + An implementation of the SessionStore protocol in the
        `ring.middleware.session.store` namespace. This determines how the
        session is stored.
      + Defaults to in-memory storage using
        `ring.middleware.session.store/memory-store`.

  - `:root`
      + The root path of the session. Any path above this will not be able to
        see this session. Equivalent to setting the cookie's path attribute.
      + Defaults to \"/\".

  - `:cookie-name`
      + The name of the cookie that holds the session key.
      + Defaults to \"ring-session\".

  - `:cookie-attrs`
      + A map of attributes to associate with the session cookie.
      + Defaults to `{:http-only true}`. This may be overridden on a
        per-response basis by adding `:session-cookie-attrs` to the response.

  NOTE: Includes [[wrap-cookies]] behaviour.
  "
  (as-handler-fn `wrap-session session/wrap-session
                 {:tags [::wrap-session]}))

;; `wrap-session` includes `wrap-cookies`
(derive `wrap-session `wrap-cookies)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([])}
  wrap-flash
  "If a `:flash` key is set on the response by the handler, a `:flash` key with
  the same value will be set on the next request that shares the same session.
  This is useful for small messages that persist across redirects. Requires
  [[wrap-session]]."
  (as-handler-fn `wrap-flash (without-options flash/wrap-flash)
                 {:tags [::wrap-flash]
                  :requires {:request [`wrap-session]}}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
