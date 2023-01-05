(ns strojure.ring-control.config.ring-ssl
  "Configuration functions for the middlewares from the `ring.middleware`
  namespace in `ring/ring-ssl` package.

  NOTE: Requires `ring/ring-ssl` to be added in project dependencies.
  "
  (:require [ring.middleware.ssl :as ssl]))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn request-forwarded-scheme
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
                   (:header header ssl/default-scheme-header))]
      {:name `request-forwarded-scheme
       :enter (fn [request] (ssl/forwarded-scheme-request request header))})))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn response-hsts
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
      {:name `response-hsts
       :leave (fn [response _] (ssl/hsts-response response options))})))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

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
       :wrap (fn [handler] (ssl/wrap-ssl-redirect handler options))})))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
