(ns strojure.ring-control.config.ring-middleware-defaults
  "Configuration functions for the [ring.middleware.defaults][1] namespace.

  NOTE: The [ring/ring-defaults][1] package should be added in project
        dependencies explicitly.

  [1]: https://clojars.org/ring/ring-defaults
  "
  (:refer-clojure :exclude [proxy])
  (:require [strojure.ring-control.config.ring-middleware :as ring]))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn- as-seq [x]
  (when-not (false? x)
    (if (sequential? x) x [x])))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn config
  "Converts [ring-defaults][1] configuration to the wrap config sequence to be
  used in `handler/wrap` function. The returned sequence can be manipulated
  before further usage.

  [1]: https://clojars.org/ring/ring-defaults
  "
  [{:keys [proxy security responses static cookies params session]}]
  (->> [[(ring/request-forwarded-remote-addr (-> proxy boolean))
         (ring/request-forwarded-scheme,,,,, (-> proxy boolean))

         (ring/wrap-ssl-redirect,,,,,,,,,,,, (-> security (:ssl-redirect false)))
         (ring/response-hsts,,,,,,,,,,,,,,,, (-> security (:hsts false)))
         (ring/response-content-type-options (-> security (:content-type-options false)))
         (ring/response-frame-options,,,,,,, (-> security (:frame-options false)))
         (ring/response-xss-protection,,,,,, (-> security (:xss-protection false)))

         (ring/response-not-modified,,,,,, (-> responses (:not-modified-responses false)))
         (ring/response-default-charset,,, (-> responses (:default-charset false)))
         (ring/response-content-type,,,,,, (-> responses (:content-types false)))]

        (map ring/wrap-file,,,, (-> static (:files false) as-seq))
        (map ring/wrap-resource (-> static (:resources false) as-seq))

        [(ring/response-absolute-redirects (-> responses (:absolute-redirects false)))

         (ring/wrap-cookies (or cookies false))

         (ring/request-params,,,,,,,,,, (-> params (:urlencoded false)))
         (ring/request-multipart-params (-> params (:multipart false)))
         (ring/request-nested-params,,, (-> params (:nested false)))
         (ring/request-keyword-params,, (-> params (:keywordize false)))

         (ring/wrap-session (-> session (or false)))
         (ring/wrap-flash,, (-> session (:flash false)))

         (ring/wrap-anti-forgery (-> security (:anti-forgery false)))]]

       (apply concat)
       (filterv some?)))

(comment
  (config {:proxy true :security {:ssl-redirect true} :cookies true
           :params {:urlencoded true}})
  (config {:static {:resources ["public"] :files "/files"}})
  )

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
