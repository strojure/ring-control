(ns usage.ring-defaults
  (:require [strojure.ring-control.config.ring-anti-forgery :as anti]
            [strojure.ring-control.config.ring-core :as ring]
            [strojure.ring-control.config.ring-headers :as headers]
            [strojure.ring-control.config.ring-ssl :as ssl]
            [strojure.ring-control.handler :as handler]))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn ring-defaults
  [{:keys [proxy security responses static cookies params session]}]
  (->> [(handler/wrap-request [(headers/request-forwarded-remote-addr (boolean proxy))
                               (ssl/request-forwarded-scheme,,,,, (boolean proxy))])
        (ssl/wrap-ssl-redirect (:ssl-redirect security false))
        (handler/wrap-response [(ring/response-content-type,,,,,,, (:content-types responses false))
                                (headers/response-default-charset,,,,, (:default-charset responses false))
                                (ring/response-not-modified,,,,,,, (:not-modified-responses responses false))
                                (headers/response-xss-protection,,,,,, (:xss-protection security false))
                                (headers/response-frame-options,,,,,,, (:frame-options security false))
                                (headers/response-content-type-options (:content-type-options security false))
                                (ssl/response-hsts,,,,,,,,,,,,,,,, (:hsts security false))])
        ;; TODO: wrap files
        ;; TODO: wrap resources
        (handler/wrap-response [(headers/response-absolute-redirects,, (:absolute-redirects responses false))])
        (ring/wrap-cookies (or cookies false))
        (handler/wrap-request [(ring/request-params,,,,,,,,,, (:urlencoded params false))
                               (ring/request-multipart-params (:multipart params false))
                               (ring/request-nested-params,,, (:nested params false))
                               (ring/request-keyword-params,, (:keywordize params false))])
        (ring/wrap-session (or session false))
        (ring/wrap-flash (:flash session false))
        (anti/wrap-anti-forgery (:anti-forgery security false))]
       (filterv some?)))

(ring-defaults {:proxy true :security {:ssl-redirect true} :cookies true
                :params {:urlencoded true}})
(ring-defaults {:proxy false})
(def -c {:proxy true :security {:ssl-redirect true} :cookies true
         :params {:urlencoded true}})
(def -c {:params {:urlencoded true
                  :keywordize true}
         :responses {:not-modified-responses true
                     :absolute-redirects true
                     :content-types true
                     :default-charset "utf-8"}})
(def -c {:params {:urlencoded true
                  :keywordize true}
         :responses {:not-modified-responses true
                     :absolute-redirects true
                     :content-types true
                     :default-charset "utf-8"}
         :security {:ssl-redirect true
                    :hsts true}})
(def -c {:params {:urlencoded true
                  :multipart true
                  :nested true
                  :keywordize true}
         :cookies true
         :session {:flash true
                   :cookie-attrs {:http-only true, :same-site :strict}}
         :security {:anti-forgery true
                    :xss-protection {:enable? true, :mode :block}
                    :frame-options :sameorigin
                    :content-type-options :nosniff}
         :static {:resources "public"}
         :responses {:not-modified-responses true
                     :absolute-redirects true
                     :content-types true
                     :default-charset "utf-8"}})
(def -h (handler/build2 identity (ring-defaults -c)))
(-h {:scheme :http :uri "/index.html" :request-method :get :headers {"host" "localhost"}})

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
