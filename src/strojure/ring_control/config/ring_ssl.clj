(ns strojure.ring-control.config.ring-ssl
  "Configuration functions for the middlewares from the `ring.middleware`
  namespace in `ring/ring-ssl` package.

  NOTE: Requires `ring/ring-ssl` to be added in project dependencies.
  "
  (:require [ring.middleware.ssl :as ssl]
            [strojure.ring-control.config.ring-core :as ring]))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([& {:keys [header]}])}
  request-forwarded-scheme
  "Returns request function map to change the `:scheme` of the request to the
  value present in a request header. This is useful if your application sits
  behind a reverse proxy or load balancer that handles the SSL transport.

  The `:header` option defaults to `x-forwarded-proto`.
  "
  (ring/with-options (fn request-forwarded-scheme [request & {:keys [header]}]
                       (ssl/forwarded-scheme-request request (or header ssl/default-scheme-header)))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([& {:keys [max-age, include-subdomains?]}])}
  response-hsts
  "Returns response function map to add the `Strict-Transport-Security` header
  to the response. This ensures the browser will only use HTTPS for future
  requests to the domain.

  Accepts the following options:

  - `:max-age` – the max time in seconds the HSTS policy applies (defaults to
                 31536000 seconds, or 1 year)

  - `:include-subdomains?` – true if subdomains should be included in the HSTS
                             policy (defaults to true)

  See RFC 6797 for more information (https://tools.ietf.org/html/rfc6797).
  "
  (ring/with-options ssl/hsts-response))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([& {:keys [ssl-port]}])}
  wrap-ssl-redirect
  "Returns handler wrapper map to redirect any HTTP request to the equivalent
  HTTPS URL.

  Accepts the following options:

  - `:ssl-port` – the SSL port to use for redirects, defaults to 443.
  "
  (ring/with-options ssl/wrap-ssl-redirect))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
