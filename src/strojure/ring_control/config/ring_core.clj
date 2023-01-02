(ns strojure.ring-control.config.ring-core
  "Configuration functions for the middlewares from the `ring.middleware`
  namespace in `ring/ring-core` package."
  (:require [ring.middleware.content-type :as content-type]
            [ring.middleware.cookies :as cookies]
            [ring.middleware.file :as file]
            [ring.middleware.flash :as flash]
            [ring.middleware.head :as head]
            [ring.middleware.keyword-params :as keyword-params]
            [ring.middleware.multipart-params :as multipart-params]
            [ring.middleware.nested-params :as nested-params]
            [ring.middleware.not-modified :as not-modified]
            [ring.middleware.params :as params]
            [ring.middleware.resource :as resource]
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

(def ^{:arglists '([& {:keys [encoding]}])}
  params-request
  "Parses urlencoded parameters from the query string and form body (if the
  request is an url-encoded form). Adds the following keys to the request map:

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

(def ^{:arglists '([& {:keys [encoding, fallback-encoding, store, progress-fn]}])}
  multipart-params-request
  "Parses multipart parameters from a request. Adds the following keys to the
  request map:

  - `:multipart-params` - a map of multipart parameters
  - `:params`           - a merged map of all types of parameter

  The following options are accepted

  - `:encoding`
      + Character encoding to use for multipart parsing.
      + Overrides the encoding specified in the request.
      + If not specified, uses the encoding specified in a part named
        \"_charset_\", or the content type for each part, or request character
        encoding if the part has no encoding, or \"UTF-8\" if no request
        character encoding is set.

  - `:fallback-encoding`
      + Specifies the character encoding used in parsing if a part of the
        request does not specify encoding in its content type or no part named
        \"_charset_\" is present.
      + Has no effect if `:encoding` is also set.

  - `:store`
      + A function that stores a file upload. The function should expect a map
        with `:filename`, `:content-type` and `:stream` keys, and its return
        value will be used as the value for the parameter in the multipart
        parameter map.
      + The default storage function is the `temp-file-store`.

  - `:progress-fn`
      + A function that gets called during uploads. The function should expect
        four parameters: `request`, `bytes-read`, `content-length`, and
        `item-count`.
  "
  (as-request-fn `multipart-params-request multipart-params/multipart-params-request
                 {:tags [::multipart-params-request]}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([& {:keys [key-parser]}])}
  nested-params-request
  "Converts a flat map of parameters into a nested map. Accepts the following
  options:

  - `:key-parser` – the function to use to parse the parameter names into a list
                    of keys. Keys that are empty strings are treated as elements
                    in a vector, non-empty keys are treated as elements in a
                    map. Defaults to the `parse-nested-keys` function.

  For example:

      {\"foo[bar]\" \"baz\"}
      => {\"foo\" {\"bar\" \"baz\"}}

      {\"foo[]\" \"bar\"}
      => {\"foo\" [\"bar\"]}
  "
  (as-request-fn `nested-params-request nested-params/nested-params-request
                 {:tags [::nested-params-request]}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([& {:keys [parse-namespaces?]}])}
  keyword-params-request
  "Converts any string keys in the `:params` map to keywords. Only keys that can
  be turned into valid keywords are converted.

  This middleware does not alter the maps under `:*-params` keys. These are left
  as strings.

  Accepts the following options:

  - `:parse-namespaces?` – if true, parse the parameters into namespaced
                           keywords (defaults to false)
  "
  (as-request-fn `keyword-params-request keyword-params/keyword-params-request
                 {:tags [::keyword-params-request]}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([& {:keys [mime-types]}])}
  content-type-response
  "Adds a `content-type` header to the response if one is not set by the
  handler. Uses the `ring.util.mime-type/ext-mime-type` function to guess the
  content-type from the file extension in the URI. If no content-type can be
  found, it defaults to 'application/octet-stream'.

  Accepts the following options:

  - `:mime-types` – a map of filename extensions to mime-types that will be
                    used in addition to the ones defined in
                    `ring.util.mime-type/default-mime-types`
  "
  (as-response-fn `content-type-response content-type/content-type-response
                  {:tags [::content-type-response]}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([])}
  not-modified-response
  "Returns a `304 Not Modified` from the wrapped handler if the handler response
  has an `ETag` or `Last-Modified` header, and the request has a `If-None-Match`
  or `If-Modified-Since` header that matches the response."
  (as-response-fn `not-modified-response (without-options not-modified/not-modified-response)
                  {:tags [::not-modified-response]}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([& {:keys [decoder, encoder]}])}
  cookies-handler
  "Parses the cookies in the request map, then assocs the resulting map
  to the `:cookies` key on the request.

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
  (as-handler-fn `cookies-handler cookies/wrap-cookies
                 {:tags [::cookies-handler]}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([& {:keys [store, root, cookie-name, cookie-attrs]}])}
  session-handler
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

  NOTE: Includes [[cookies-handler]] behaviour.
  "
  (as-handler-fn `session-handler session/wrap-session
                 {:tags [::session-handler]}))

;; `session-handler` includes `cookies-handler`
(derive `session-handler `cookies-handler)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([])}
  flash-handler
  "If a `:flash` key is set on the response by the handler, a `:flash` key with
  the same value will be set on the next request that shares the same session.
  This is useful for small messages that persist across redirects. Requires
  [[session-handler]]."
  (as-handler-fn `flash-handler (without-options flash/wrap-flash)
                 {:tags [::flash-handler]
                  :requires {:request [`session-handler]}}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([& {:keys [root-path, index-files?, allow-symlinks?, prefer-handler?]}])}
  file-handler
  "Wrap a handler such that the directory at the given `:root-path` is checked
  for a static file with which to respond to the request, proxying the request
  to the wrapped handler if such a file does not exist.

  Accepts the following options:

  - `:root-path`
  - `:index-files?`    – look for index.* files in directories, defaults to true
  - `:allow-symlinks?` – serve files through symbolic links, defaults to false
  - `:prefer-handler?` – prioritize handler response over files, defaults to
                         false
  "
  (as-handler-fn `file-handler (fn [handler {:as options :keys [root-path]}]
                                 (file/wrap-file handler root-path options))
                 {:tags [::file-handler]
                  :requires {:request [`file-handler]}}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([& {:keys [root-path, loader, allow-symlinks?, prefer-handler?]}])}
  resource-handler
  "Middleware that first checks to see whether the request map matches a static
  resource. If it does, the resource is returned in a response map, otherwise
  the request map is passed onto the handler. The `:root-path` argument will be
  added to the beginning of the resource path.

  Accepts the following options:

  - `:root-path`
  - `:loader`          – resolve the resource using this class loader
  - `:allow-symlinks?` – allow symlinks that lead to paths outside the root
                         classpath directories (defaults to false)
  - `:prefer-handler?` – prioritize handler response over resources (defaults to
                         false)
  "
  (as-handler-fn `resource-handler (fn [handler {:as options :keys [root-path]}]
                                     (resource/wrap-resource handler root-path options))
                 {:tags [::resource-handler]
                  :requires {:request [`resource-handler]}}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([])}
  head-handler
  "Middleware that turns any HEAD request into a GET, and then sets the response
  body to `nil`."
  (as-handler-fn `head-handler (without-options head/wrap-head)
                 {:tags [::head-handler]}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
