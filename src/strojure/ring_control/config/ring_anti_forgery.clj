(ns strojure.ring-control.config.ring-anti-forgery
  (:require [ring.middleware.anti-forgery :as anti-forgery]
            [strojure.ring-control.config.ring-core :as ring]))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([& {:keys [read-token, error-response, error-handler, strategy]}])}
  wrap-anti-forgery
  ;; TODO: reformat docstring
  "Middleware that prevents CSRF attacks. Any POST request to the handler
  returned by this function must contain a valid anti-forgery token, or else an
  access-denied response is returned.

  The anti-forgery token can be placed into a HTML page via the
  *anti-forgery-token* var, which is bound to a (possibly deferred) token.
  The token is also available in the request under
  `:anti-forgery-token`.

  By default, the token is expected to be POSTed in a form field named
  '__anti-forgery-token', or in the 'X-CSRF-Token' or 'X-XSRF-Token'
  headers.

  Accepts the following options:

  :read-token     - a function that takes a request and returns an anti-forgery
                    token, or nil if the token does not exist

  :error-response - the response to return if the anti-forgery token is
                    incorrect or missing

  :error-handler  - a handler function to call if the anti-forgery token is
                    incorrect or missing

  :strategy       - a strategy for creating and validating anti-forgety tokens,
                    which must satisfy the
                    ring.middleware.anti-forgery.strategy/Strategy protocol
                    (defaults to the session strategy:
                    ring.middleware.anti-forgery.session/session-strategy)

  Only one of :error-response, :error-handler may be specified."
  (ring/with-options anti-forgery/wrap-anti-forgery))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
