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

(def ^{:arglists '([& {:keys [frame-options]}])}
  frame-options-response
  "Adds the X-Frame-Options header to the response. This governs whether your
  site can be rendered in a <frame>, <iframe> or <object>, and is typically used
  to prevent clickjacking attacks.

  The following `:frame-options` values are allowed:

  - `:deny`             – prevent any framing of the content
  - `:sameorigin`       – allow only the current site to frame the content
  - `{:allow-from uri}` – allow only the specified URI to frame the page

  The `:deny` and `:sameorigin` options are keywords, while the `:allow-from`
  option is a map consisting of one key/value pair.

  Note that browser support for `:allow-from` is incomplete. See:
  https://developer.mozilla.org/en-US/docs/Web/HTTP/X-Frame-Options
  "
  (ring/as-response-fn `frame-options-response
                       (fn [response _ {:keys [frame-options]}]
                         (assert (or (= frame-options :deny)
                                     (= frame-options :sameorigin)
                                     (#'x-headers/allow-from? frame-options)))
                         (x-headers/frame-options-response response frame-options))
                       {:tags [::frame-options-response]}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([& {:keys [content-type-options]}])}
  content-type-options-response
  "Adds the `X-Content-Type-Options` header to the response. This currently only
  accepts one option:

  - `:nosniff – prevent resources with invalid media types being loaded as
                stylesheets or scripts

  This prevents attacks based around media type confusion. See:
  http://msdn.microsoft.com/en-us/library/ie/gg622941(v=vs.85).aspx
  "
  (ring/as-response-fn `content-type-options-response
                       (fn [response _ {:keys [content-type-options]}]
                         (assert (= content-type-options :nosniff))
                         (x-headers/content-type-options-response response content-type-options))
                       {:tags [::content-type-options-response]}))

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
