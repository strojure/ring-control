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
  request-forwarded-remote-addr
  "Changes the `:remote-addr` key of the request map to the last value present
  in the `X-Forwarded-For` header."
  (ring/with-options proxy-headers/forwarded-remote-addr-request))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([])}
  response-absolute-redirects
  "Convert a response that redirects to a relative URLs into a response that
  redirects to an absolute URL. While many browsers can handle relative URLs in
  the Location header, RFC 2616 states that the Location header must contain an
  absolute URL."
  (ring/with-options absolute-redirects/absolute-redirects-response))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([charset])}
  response-default-charset
  "Add a default charset to a response if the response has no charset and
  requires one."
  (fn [charset]
    (when charset
      (fn [response _]
        (default-charset/default-charset-response response charset)))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([frame-options])}
  response-frame-options
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
  (fn [frame-options]
    (when frame-options
      (assert (or (= frame-options :deny)
                  (= frame-options :sameorigin)
                  (#'x-headers/allow-from? frame-options)))
      (fn [response _]
        ;; TODO: review performance
        (x-headers/frame-options-response response frame-options)))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([content-type-options])}
  response-content-type-options
  "Adds the `X-Content-Type-Options` header to the response. This currently only
  accepts one option:

  - `:nosniff – prevent resources with invalid media types being loaded as
                stylesheets or scripts

  This prevents attacks based around media type confusion. See:
  http://msdn.microsoft.com/en-us/library/ie/gg622941(v=vs.85).aspx
  "
  (fn [content-type-options]
    (when content-type-options
      (assert (= content-type-options :nosniff))
      (fn [response _]
        ;; TODO: review performance
        (x-headers/content-type-options-response response content-type-options)))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(def ^{:arglists '([& {:keys [enable? mode]}])}
  response-xss-protection
  "Add the `X-XSS-Protection` header to the response. This header enables a
  heuristic filter in browsers for detecting cross-site scripting attacks.

  - `:enable?` – determines whether the filter should be turned on
  - `:mode`    – currently accepts only `:block`

  See: http://msdn.microsoft.com/en-us/library/dd565647(v=vs.85).aspx
  "
  (fn [& {:keys [enable? mode] :as options}]
    (when-not (false? options)
      (assert (or (nil? mode) (= :block mode)) "currently accepts only :block")
      (let [options (not-empty (select-keys options [:mode]))]
        (fn [response _]
          ;; TODO: review performance
          (x-headers/xss-protection-response response enable? options))))))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
