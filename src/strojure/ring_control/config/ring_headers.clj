(ns strojure.ring-control.config.ring-headers
  "Configuration functions for the middlewares from the `ring.middleware`
  namespace in `ring/ring-headers` package.

  NOTE: Requires `ring/ring-headers` to be added in project dependencies.
  "
  (:require [ring.middleware.absolute-redirects :as absolute-redirects]
            [ring.middleware.default-charset :as default-charset]
            [ring.middleware.proxy-headers :as proxy-headers]
            [ring.middleware.x-headers :as x-headers]
            [strojure.ring-control.config.ring-core :as ring]))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([])}
  forwarded-remote-addr-request
  "Changes the `:remote-addr` key of the request map to the last value present
  in the `X-Forwarded-For` header."
  (ring/as-request-fn `forwarded-remote-addr-request
                      (ring/without-options proxy-headers/forwarded-remote-addr-request)
                      {:tags [::forwarded-remote-addr-request]}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([])}
  absolute-redirects-response
  "Convert a response that redirects to a relative URLs into a response that
  redirects to an absolute URL. While many browsers can handle relative URLs in
  the Location header, RFC 2616 states that the Location header must contain an
  absolute URL."
  (ring/as-response-fn `absolute-redirects-response
                       (ring/without-options absolute-redirects/absolute-redirects-response)
                       {:tags [::absolute-redirects-response]}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([& {:keys [charset]}])}
  default-charset-response
  "Add a default charset to a response if the response has no charset and
  requires one."
  (ring/as-response-fn `default-charset-response
                       (fn [response _ {:keys [charset]}]
                         (default-charset/default-charset-response response charset))
                       {:tags [::default-charset-response]}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([& {:keys [enable? mode]}])}
  xss-protection-response
  "Add the `X-XSS-Protection` header to the response. This header enables a
  heuristic filter in browsers for detecting cross-site scripting attacks.

  - `:enable?` – determines whether the filter should be turned on
  - `:mode`    – currently accepts only `:block`

  See: http://msdn.microsoft.com/en-us/library/dd565647(v=vs.85).aspx
  "
  (ring/as-response-fn `xss-protection-response
                       (fn [response _ {:keys [enable? mode]}]
                         (assert (or (nil? mode) (= :block mode)) "currently accepts only :block")
                         (x-headers/xss-protection-response response enable? (when mode {:mode mode})))
                       {:tags [::xss-protection-response]}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
