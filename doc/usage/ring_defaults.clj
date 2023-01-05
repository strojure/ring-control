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
  (->> [(headers/request-forwarded-remote-addr (boolean proxy))
        (ssl/request-forwarded-scheme,,,,,,,,, (boolean proxy))

        (ssl/wrap-ssl-redirect,,,,,,,,,,,,,,,, (-> security (:ssl-redirect false)))
        (ssl/response-hsts,,,,,,,,,,,,,,,,,,,, (-> security (:hsts false)))
        (headers/response-content-type-options (-> security (:content-type-options false)))
        (headers/response-frame-options,,,,,,, (-> security (:frame-options false)))
        (headers/response-xss-protection,,,,,, (-> security (:xss-protection false)))

        (ring/response-not-modified,,,,,,,,, (-> responses (:not-modified-responses false)))
        (headers/response-default-charset,,, (-> responses (:default-charset false)))
        (ring/response-content-type,,,,,,,,, (-> responses (:content-types false)))

        ;; TODO: wrap files
        ;; TODO: wrap resources

        (headers/response-absolute-redirects (-> responses (:absolute-redirects false)))

        (ring/wrap-cookies (or cookies false))
        (ring/request-params,,,,,,,,,, (-> params (:urlencoded false)))
        (ring/request-multipart-params (-> params (:multipart false)))
        (ring/request-nested-params,,, (-> params (:nested false)))
        (ring/request-keyword-params,, (-> params (:keywordize false)))

        (ring/wrap-session (-> session (or false)))
        (ring/wrap-flash,, (-> session (:flash false)))

        (anti/wrap-anti-forgery (-> security (:anti-forgery false)))]
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
(def -h (handler/wrap identity (ring-defaults -c)))
(-h {:scheme :https :uri "/index.html" :request-method :get :headers {"host" "localhost"}})

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
