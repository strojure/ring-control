(ns strojure.ring-control.config.ring-middleware)

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn- resolve-all
  "Returns sequence of all symbols resolved or `nil`."
  [& xs]
  (reduce (fn [res sym] (if-let [v (resolve sym)]
                          (conj res v)
                          (reduced nil)))
          [] xs))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(when-let [request-fn (resolve 'ring.middleware.params/params-request)]

  (defn request-params
    "Middleware to parse urlencoded parameters from the query string and form body
    (if the request is an url-encoded form). Adds the following keys to the
    request map:

    - `:query-params` – a map of parameters from the query string
    - `:form-params`  – a map of parameters from the body
    - `:params`       – a merged map of all types of parameter

    Accepts the following options:

    - `:encoding` – encoding to use for url-decoding. If not specified, uses
                    the request character encoding, or \"UTF-8\" if no request
                    character encoding is set.
    "
    {:arglists '([& {:keys [encoding]}]
                 [false])}
    [& {:as options}]
    (when-not (false? options)
      (let [options (or options {})]
        {:name `request-params
         :enter (fn enter [request] (request-fn request options))}))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(when-let [request-fn (resolve 'ring.middleware.multipart-params/multipart-params-request)]

  (defn request-multipart-params
    "Middleware to parse multipart parameters from a request. Adds the following
    keys to the request map:

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
    {:arglists '([& {:keys [encoding, fallback-encoding, store, progress-fn]}]
                 [false])}
    [& {:as options}]
    (when-not (false? options)
      (let [options (or options {})]
        {:name `request-multipart-params
         :enter (fn enter [request] (request-fn request options))}))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(when-let [request-fn (resolve 'ring.middleware.nested-params/nested-params-request)]

  (defn request-nested-params
    "Middleware to convert a flat map of parameters into a nested map. Accepts the
    following options:

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
    {:arglists '([& {:keys [key-parser]}]
                 [false])}
    [& {:as options}]
    (when-not (false? options)
      (let [options (or options {})]
        {:name `request-nested-params
         :enter (fn enter [request] (request-fn request options))}))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(when-let [request-fn (resolve 'ring.middleware.keyword-params/keyword-params-request)]

  (defn request-keyword-params
    "Middleware that converts any string keys in the `:params` map to keywords.
    Only keys that can be turned into valid keywords are converted.

    This middleware does not alter the maps under `:*-params` keys. These are left
    as strings.

    Accepts the following options:

    - `:parse-namespaces?` – if true, parse the parameters into namespaced
                             keywords (defaults to false)
    "
    {:arglists '([& {:keys [parse-namespaces?]}]
                 [false])}
    [& {:as options}]
    (when-not (false? options)
      (let [options (or options {})]
        {:name `request-keyword-params
         :enter (fn enter [request] (request-fn request options))}))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(when-let [response-fn (resolve 'ring.middleware.content-type/content-type-response)]

  (defn response-content-type
    "Middleware that adds a `content-type` header to the response if one is not
    set by the handler. Uses the `ring.util.mime-type/ext-mime-type` function to
    guess the content-type from the file extension in the URI. If no content-type
    can be found, it defaults to 'application/octet-stream'.

    Accepts the following options:

    - `:mime-types` – a map of filename extensions to mime-types that will be
                      used in addition to the ones defined in
                      `ring.util.mime-type/default-mime-types`
    "
    {:arglists '([& {:keys [mime-types]}]
                 [false])}
    [& {:as options}]
    (when-not (false? options)
      (let [options (or options {})]
        {:name `response-content-type
         :leave (fn leave [response request] (response-fn response request options))}))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(when-let [response-fn (resolve 'ring.middleware.not-modified/not-modified-response)]

  (defn response-not-modified
    "Middleware that returns a `304 Not Modified` from the wrapped handler if the
    handler response has an `ETag` or `Last-Modified` header, and the request has
    a `If-None-Match` or `If-Modified-Since` header that matches the response."
    {:arglists '([] [false])}
    [& {:as options}]
    (when-not (false? options)
      {:name `response-not-modified
       :leave response-fn})))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(when-let [[request-fn response-fn] (resolve-all 'ring.middleware.cookies/cookies-request
                                                 'ring.middleware.cookies/cookies-response)]

  (defn wrap-cookies
    "Parses the cookies in the request map, then assoc the resulting map to the
    `:cookies` key on the request.

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
    {:arglists '([& {:keys [decoder, encoder]}])}
    [& {:as options}]
    (when-not (false? options)
      {:name `wrap-cookies
       :enter (fn enter [request] (request-fn request options))
       :leave (fn leave [response _] (response-fn response options))})))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(when-let [[request-fn response-fn options-fn]
           (resolve-all 'ring.middleware.session/session-request
                        'ring.middleware.session/session-response
                        'ring.middleware.session/session-options)]
  (defn wrap-session
    "Reads in the current HTTP session map, and add it to the `:session` key on
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
    {:arglists '([& {:keys [store, root, cookie-name, cookie-attrs]}]
                 [false])}
    [& {:as options}]
    (when-not (false? options)
      (let [options (options-fn (or options {}))]
        {:name `wrap-session
         :enter (fn enter [request] (request-fn request options))
         :leave (fn leave [response request] (response-fn response request options))}))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
