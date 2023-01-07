(ns strojure.ring-control.config.ring-middleware
  "Configuration functions for the middlewares from the `ring.middleware`
  namespace in the packages:

  - [ring/ring-core](https://clojars.org/ring/ring-core)
  - [ring/ring-headers](https://clojars.org/ring/ring-headers)
  - [ring/ring-ssl](https://clojars.org/ring/ring-ssl)
  - [ring/ring-anti-forgery](https://clojars.org/ring/ring-anti-forgery)
  - [ring/ring-devel](https://clojars.org/ring/ring-devel)

  NOTE: Requires corresponding packages to be added in project dependencies
        explicitly.
  ")

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn- resolve*
  "Returns sequence of all symbols resolved or `nil`. Loads resolving libs."
  [& xs]
  (reduce (fn [res sym]
            (if-let [v (try (requiring-resolve sym)
                            (catch Throwable _))]
              (conj res (deref v))
              (reduced nil)))
          [] xs))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(declare req-params)                              ; ring/ring-core

(when-let [[request-fn] (resolve* 'ring.middleware.params/params-request)]

  (defn req-params
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
        {:name `req-params
         :enter (fn enter [request] (request-fn request options))}))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(declare req-multipart-params)                    ; ring/ring-core

(when-let [[request-fn] (resolve* 'ring.middleware.multipart-params/multipart-params-request)]

  (defn req-multipart-params
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
        {:name `req-multipart-params
         :enter (fn enter [request] (request-fn request options))}))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(declare req-nested-params)                       ; ring/ring-core

(when-let [[request-fn] (resolve* 'ring.middleware.nested-params/nested-params-request)]

  (defn req-nested-params
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
        {:name `req-nested-params
         :enter (fn enter [request] (request-fn request options))}))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(declare req-keyword-params)                      ; ring/ring-core

(when-let [[request-fn] (resolve* 'ring.middleware.keyword-params/keyword-params-request)]

  (defn req-keyword-params
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
        {:name `req-keyword-params
         :enter (fn enter [request] (request-fn request options))
         :deps {`req-params :before}}))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(declare req-forwarded-remote-addr)               ; ring/ring-headers

(when-let [[request-fn] (resolve* 'ring.middleware.proxy-headers/forwarded-remote-addr-request)]

  (defn req-forwarded-remote-addr
    "Middleware that changes the `:remote-addr` key of the request map to the last
    value present in the `X-Forwarded-For` header."
    {:arglists '([] [false])}
    [& {:as options}]
    (when-not (false? options)
      {:name `req-forwarded-remote-addr
       :enter request-fn})))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(declare req-forwarded-scheme)                    ; ring/ring-ssl

(when-let [[request-fn default-header] (resolve* 'ring.middleware.ssl/forwarded-scheme-request
                                                 'ring.middleware.ssl/default-scheme-header)]

  (defn req-forwarded-scheme
    "Middleware that changes the `:scheme` of the request to the value present in
    a request header. This is useful if your application sits behind a reverse
    proxy or load balancer that handles the SSL transport.

    The `header` defaults to `x-forwarded-proto`.
    "
    {:arglists '([] [header] [false])}
    [& {:as header}]
    (when-not (false? header)
      (let [header (if (string? header)
                     header
                     (:header header default-header))]
        {:name `req-forwarded-scheme
         :enter (fn [request] (request-fn request header))}))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(declare resp-content-type)                       ; ring/ring-core

(when-let [[response-fn] (resolve* 'ring.middleware.content-type/content-type-response)]

  (defn resp-content-type
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
        {:name `resp-content-type
         :leave (fn leave [response request] (response-fn response request options))}))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(declare resp-not-modified)                       ; ring/ring-core

(when-let [[response-fn] (resolve* 'ring.middleware.not-modified/not-modified-response)]

  (defn resp-not-modified
    "Middleware that returns a `304 Not Modified` from the wrapped handler if the
    handler response has an `ETag` or `Last-Modified` header, and the request has
    a `If-None-Match` or `If-Modified-Since` header that matches the response."
    {:arglists '([] [false])}
    [& {:as options}]
    (when-not (false? options)
      {:name `resp-not-modified
       :leave response-fn})))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(declare resp-absolute-redirects)                 ; ring/ring-headers

(when-let [[response-fn] (resolve* 'ring.middleware.absolute-redirects/absolute-redirects-response)]

  (defn resp-absolute-redirects
    "Middleware that converts redirects to relative URLs into redirects to
    absolute URLs. While many browsers can handle relative URLs in the Location
    header, RFC 2616 states that the Location header must contain an absolute
    URL."
    {:arglists '([] [false])}
    [& {:as options}]
    (when-not (false? options)
      {:name `resp-absolute-redirects
       :leave response-fn})))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(declare resp-default-charset)                    ; ring/ring-headers

(when-let [[response-fn] (resolve* 'ring.middleware.default-charset/default-charset-response)]

  (defn resp-default-charset
    "Middleware that adds a charset to the `content-type` header of the response
    if one was not set by the handler."
    {:arglists '([charset] [false])}
    [charset]
    (when charset
      {:name `resp-default-charset
       :leave (fn leave [response _] (response-fn response charset))})))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(declare resp-frame-options)                      ; ring/ring-headers

(when-let [[response-fn allow-from?] (resolve* 'ring.middleware.x-headers/frame-options-response
                                               'ring.middleware.x-headers/allow-from?)]

  (defn resp-frame-options
    "Middleware that adds the `X-Frame-Options` header to the response. This
    governs whether your site can be rendered in a <frame>, <iframe> or <object>,
    and is typically used to prevent clickjacking attacks.

    The following `frame-options` values are allowed:

    - `:deny`             – prevent any framing of the content
    - `:sameorigin`       – allow only the current site to frame the content
    - `{:allow-from uri}` – allow only the specified URI to frame the page

    The `:deny` and `:sameorigin` options are keywords, while the `:allow-from`
    option is a map consisting of one key/value pair.

    Note that browser support for `:allow-from` is incomplete. See:
    https://developer.mozilla.org/en-US/docs/Web/HTTP/X-Frame-Options
    "
    {:arglists '([frame-options]
                 [false])}
    [frame-options]
    (when frame-options
      (assert (or (= frame-options :deny)
                  (= frame-options :sameorigin)
                  (allow-from? frame-options)))
      {:name `resp-frame-options
       :leave (fn leave [response _]
                ;; TODO: review performance
                (response-fn response frame-options))})))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(declare resp-content-type-options)               ; ring/ring-headers

(when-let [[response-fn] (resolve* 'ring.middleware.x-headers/content-type-options-response)]

  (defn resp-content-type-options
    "Middleware that adds the `X-Content-Type-Options` header to the response.
    This currently only accepts one option:

    - `:nosniff – prevent resources with invalid media types being loaded as
                  stylesheets or scripts

    This prevents attacks based around media type confusion. See:
    http://msdn.microsoft.com/en-us/library/ie/gg622941(v=vs.85).aspx
    "
    {:arglists '([content-type-options]
                 [false])}
    [content-type-options]
    (when content-type-options
      (assert (= content-type-options :nosniff))
      {:name `resp-content-type-options
       :leave (fn leave [response _]
                ;; TODO: review performance
                (response-fn response content-type-options))})))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(declare resp-xss-protection)                     ; ring/ring-headers

(when-let [[response-fn] (resolve* 'ring.middleware.x-headers/xss-protection-response)]

  (defn resp-xss-protection
    "Middleware that adds the `X-XSS-Protection` header to the response. This
    header enables a heuristic filter in browsers for detecting cross-site
    scripting attacks.

    - `:enable?` – determines whether the filter should be turned on
    - `:mode`    – currently accepts only `:block`

    See: http://msdn.microsoft.com/en-us/library/dd565647(v=vs.85).aspx
    "
    {:arglists '([& {:keys [enable? mode]}]
                 [false])}
    [& {:as options}]
    (when-not (false? options)
      (let [enable? (:enable options)
            options (not-empty (dissoc options :enable?))]
        (assert (or (nil? options) (= options {:mode :block})))
        {:name `resp-xss-protection
         :leave (fn leave [response _]
                  ;; TODO: review performance
                  (response-fn response enable? options))}))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(declare resp-hsts)                               ; ring/ring-ssl

(when-let [[response-fn] (resolve* 'ring.middleware.ssl/hsts-response)]

  (defn resp-hsts
    "Middleware that adds the `Strict-Transport-Security` header to the response.
    This ensures the browser will only use HTTPS for future requests to the
    domain.

    Accepts the following options:

    - `:max-age` – the max time in seconds the HSTS policy applies (defaults to
                   31536000 seconds, or 1 year)

    - `:include-subdomains?` – true if subdomains should be included in the HSTS
                               policy (defaults to true)

    See RFC 6797 for more information (https://tools.ietf.org/html/rfc6797).
    "
    {:arglists '([& {:keys [max-age, include-subdomains?]}]
                 [false])}
    [& {:as options}]
    (when-not (false? options)
      (let [options (or options {})]
        {:name `resp-hsts
         :leave (fn [response _] (response-fn response options))}))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(declare wrap-cookies)                            ; ring/ring-core

(when-let [[request-fn response-fn] (resolve* 'ring.middleware.cookies/cookies-request
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

(declare wrap-session)                            ; ring/ring-core

(when-let [[request-fn response-fn options-fn]
           (resolve* 'ring.middleware.session/session-request
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

(declare wrap-flash)                              ; ring/ring-core

(when-let [[request-fn response-fn] (resolve* 'ring.middleware.flash/flash-request
                                              'ring.middleware.flash/flash-response)]

  (defn wrap-flash
    "If a `:flash` key is set on the response by the handler, a `:flash` key with
    the same value will be set on the next request that shares the same session.
    This is useful for small messages that persist
    across redirects."
    {:arglists '([] [false])}
    [& {:as options}]
    (when-not (false? options)
      {:name `wrap-flash
       :enter request-fn
       :leave response-fn
       :deps {`wrap-session :before}})))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(declare wrap-file)                               ; ring/ring-core

(when-let [[wrap-fn] (resolve* 'ring.middleware.file/wrap-file)]

  (defn wrap-file
    "Wrap a handler such that the directory at the given `root-path` is checked
    for a static file with which to respond to the request, proxying the request
    to the wrapped handler if such a file does not exist.

    Accepts the following options:

    - `:index-files?`    – look for index.* files in directories, defaults to true
    - `:allow-symlinks?` – serve files through symbolic links, defaults to false
    - `:prefer-handler?` – prioritize handler response over files, defaults to
                           false
    "
    {:arglists '([root-path & {:keys [index-files?, allow-symlinks?, prefer-handler?]}]
                 [false])}
    [root-path & {:as options}]
    (when root-path
      (let [options (or options {})]
        {:name `wrap-file
         :meta {:root-path root-path}
         :wrap (fn [handler] (wrap-fn handler root-path options))}))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(declare wrap-resource)                           ; ring/ring-core

(when-let [[wrap-fn] (resolve* 'ring.middleware.resource/wrap-resource)]

  (defn wrap-resource
    "Middleware that first checks to see whether the request map matches a static
    resource. If it does, the resource is returned in a response map, otherwise
    the request map is passed onto the handler. The `root-path` argument will be
    added to the beginning of the resource path.

    Accepts the following options:

    - `:loader`          – resolve the resource using this class loader
    - `:allow-symlinks?` – allow symlinks that lead to paths outside the root
                           classpath directories (defaults to false)
    - `:prefer-handler?` – prioritize handler response over resources (defaults to
                           false)
    "
    {:arglists '([root-path & {:keys [loader, allow-symlinks?, prefer-handler?]}]
                 [false])}
    [root-path & {:as options}]
    (when root-path
      (let [options (or options {})]
        {:name `wrap-resource
         :meta {:root-path root-path}
         :wrap (fn [handler] (wrap-fn handler root-path options))}))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(declare wrap-head)                               ; ring/ring-core

(when-let [[request-fn response-fn] (resolve* 'ring.middleware.head/head-request
                                              'ring.middleware.head/head-response)]

  (defn wrap-head
    "Middleware that turns any HEAD request into a GET, and then sets the response
    body to `nil`."
    {:arglists '([] [false])}
    [& {:as options}]
    (when-not (false? options)
      {:name `wrap-head
       :enter request-fn
       :leave response-fn})))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(declare wrap-ssl-redirect)                       ; ring/ring-ssl

(when-let [[wrap-fn] (resolve* 'ring.middleware.ssl/wrap-ssl-redirect)]

  (defn wrap-ssl-redirect
    "Middleware that redirects any HTTP request to the equivalent HTTPS URL.

    Accepts the following options:

    - `:ssl-port` – the SSL port to use for redirects, defaults to 443.
    "
    {:arglists '([& {:keys [ssl-port]}])}
    [& {:as options}]
    (when-not (false? options)
      (let [options (or options {})]
        {:name `wrap-ssl-redirect
         :wrap (fn [handler] (wrap-fn handler options))}))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(declare wrap-anti-forgery)                       ; ring/ring-anti-forgery

(when-let [[wrap-fn] (resolve* 'ring.middleware.anti-forgery/wrap-anti-forgery)]

  (defn wrap-anti-forgery
    "Middleware that prevents CSRF attacks. Any POST request to the handler
    returned by this function must contain a valid anti-forgery token, or else an
    access-denied response is returned.

    The anti-forgery token can be placed into an HTML page via the
    `*anti-forgery-token*` var, which is bound to a (possibly deferred) token.
    The token is also available in the request under
    `:anti-forgery-token`.

    By default, the token is expected to be POSTed in a form field named
    `__anti-forgery-token`, or in the `X-CSRF-Token` or `X-XSRF-Token`
    headers.

    Accepts the following options:

    - `:read-token`
        - a function that takes a request and returns an anti-forgery token, or
          `nil` if the token does not exist.

    - `:error-response`
        - the response to return if the anti-forgery token is incorrect or missing

    - `:error-handler`
        - a handler function to call if the anti-forgery token is incorrect or
          missing

    - `:strategy`
        - a strategy for creating and validating anti-forgery tokens.
        + Must satisfy the `ring.middleware.anti-forgery.strategy/Strategy`
          protocol.
        + Defaults to the session strategy:
          `ring.middleware.anti-forgery.session/session-strategy`.

    Only one of `:error-response`, `:error-handler` may be specified.
    "
    {:arglists '([& {:keys [read-token, error-response, error-handler, strategy]}]
                 [false])}
    [& {:as options}]
    (when-not (false? options)
      (let [options (or options {})]
        {:name `wrap-anti-forgery
         :wrap (fn [handler] (wrap-fn handler options))}))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(declare wrap-lint)                               ; ring/ring-devel

(when-let [[wrap-fn] (resolve* 'ring.middleware.lint/wrap-lint)]

  (defn wrap-lint
    "Wrap a handler to validate incoming requests and outgoing responses
    according to the current Ring specification. An exception is raised if either
    the request or response is invalid."
    {:arglists '([] [false])}
    [& {:as options}]
    (when-not (false? options)
      {:name `wrap-lint
       :wrap wrap-fn})))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(declare wrap-reload)                             ; ring/ring-devel

(when-let [[wrap-fn] (resolve* 'ring.middleware.reload/wrap-reload)]

  (defn wrap-reload
    "Reload namespaces of modified files before the request is passed to the
    supplied handler.

    Accepts the following options:

    - `:dirs`
        + A list of directories that contain the source files.
        + Defaults to `[\"src\"]`.

    - `:reload-compile-errors?`
        + If true, keep attempting to reload namespaces that have compile errors.
        + Defaults to `true`.
    "
    {:arglists '([& {:keys [dirs, reload-compile-errors?]}]
                 [false])}
    [& {:as options}]
    (when-not (false? options)
      (let [options (or options {})]
        {:name `wrap-reload
         :wrap (fn [handler] (wrap-fn handler options))}))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(declare wrap-stacktrace)                         ; ring/ring-devel

(when-let [[wrap-fn] (resolve* 'ring.middleware.stacktrace/wrap-stacktrace)]

  (defn wrap-stacktrace
    "Wrap a handler such that exceptions are caught, a corresponding stacktrace is
    logged to `*err*`, and an HTML representation of the stacktrace is returned as
    a response.

    Accepts the following option:

    - `:color?` – if true, apply ANSI colors to terminal stacktrace (default false)
    "
    {:arglists '([& {:keys [color?]}])}
    [& {:as options}]
    (when-not (false? options)
      (let [options (or options {})]
        {:name `wrap-stacktrace
         :wrap (fn [handler] (wrap-fn handler options))}))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
