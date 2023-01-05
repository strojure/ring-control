(ns strojure.ring-control.config.ring-headers
  "Configuration functions for the middlewares from the `ring.middleware`
  namespace in `ring/ring-headers` package.

  NOTE: Requires `ring/ring-headers` to be added in project dependencies.
  "
  (:require [ring.middleware.absolute-redirects :as absolute-redirects]
            [ring.middleware.default-charset :as default-charset]
            [ring.middleware.proxy-headers :as proxy-headers]
            [ring.middleware.x-headers :as x-headers]))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn request-forwarded-remote-addr
  "Middleware that changes the `:remote-addr` key of the request map to the last
  value present in the `X-Forwarded-For` header."
  {:arglists '([] [false])}
  [& {:as options}]
  (when-not (false? options)
    {:name `request-forwarded-remote-addr
     :enter proxy-headers/forwarded-remote-addr-request}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn response-absolute-redirects
  "Middleware that converts redirects to relative URLs into redirects to
  absolute URLs. While many browsers can handle relative URLs in the Location
  header, RFC 2616 states that the Location header must contain an absolute
  URL."
  {:arglists '([] [false])}
  [& {:as options}]
  (when-not (false? options)
    {:name `response-absolute-redirects
     :leave absolute-redirects/absolute-redirects-response}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn response-default-charset
  "Middleware that adds a charset to the `content-type` header of the response
  if one was not set by the handler."
  {:arglists '([charset] [false])}
  [charset]
  (when charset
    {:name `response-default-charset
     :leave (fn leave [response _]
              (default-charset/default-charset-response response charset))}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn response-frame-options
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
                (#'x-headers/allow-from? frame-options)))
    {:name `response-frame-options
     :leave (fn leave [response _]
              ;; TODO: review performance
              (x-headers/frame-options-response response frame-options))}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn response-content-type-options
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
    {:name `response-content-type-options
     :leave (fn leave [response _]
              ;; TODO: review performance
              (x-headers/content-type-options-response response content-type-options))}))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn response-xss-protection
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
      {:name `response-xss-protection
       :leave (fn leave [response _]
                ;; TODO: review performance
                (x-headers/xss-protection-response response enable? options))})))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
