(ns usage.walkthrough
  (:require [strojure.ring-control.config :as config]
            [strojure.ring-control.handler :as handler]))

;; ## Define builder configuration

;; - Define standard ring handlers

(defn- wrap1 [handler]
  (fn [request]
    (println 'wrap1 request)
    (doto (-> (update request :trace/request conj 'wrap1)
              (handler)
              (update :trace/response conj 'wrap1))
      (->> (println 'wrap1)))))

(defn- wrap2 [handler]
  (fn [request]
    (println 'wrap2 request)
    (doto (-> (update request :trace/request conj 'wrap2)
              (handler)
              (update :trace/response conj 'wrap2))
      (->> (println 'wrap2)))))

(config/as-wrap-handler `wrap2 (constantly wrap2))

(defn- wrap3 [handler]
  (fn [request]
    (println 'wrap3 request)
    (doto (-> (update request :trace/request conj 'wrap3)
              (handler)
              (update :trace/response conj 'wrap3))
      (->> (println 'wrap3)))))

(defn- wrap4 [handler]
  (fn [request]
    (println 'wrap4 request)
    (doto (-> (update request :trace/request conj 'wrap4)
              (handler)
              (update :trace/response conj 'wrap4))
      (->> (println 'wrap4)))))

;; Register functions to be used in configuration.
(config/as-wrap-handler `wrap1 (constantly wrap1) {:tags [wrap1 ::wrap1]})
(config/as-wrap-handler `wrap2 (constantly wrap2) {:tags [wrap2 ::wrap2]})
(config/as-wrap-handler `wrap3 (constantly wrap3) {:tags [wrap3 ::wrap3]})
(config/as-wrap-handler `wrap4 (constantly wrap4) {:tags [wrap4 ::wrap4]})

;; - Define ring request wrappers

(defn- request1 [request]
  (println 'request1 request)
  (update request :trace/request conj 'request1))

(defn- request2 [request]
  (println 'request2 request)
  (update request :trace/request conj 'request2))

;; Register functions to be used in configuration.
(config/as-wrap-request `request1 (constantly request1) {:tags [request1 ::request1]})
(config/as-wrap-request `request2 (constantly request2) {:tags [request2 ::request2]})

;; - Define ring response wrappers

(defn- response1 [response request]
  (println 'response1 response request)
  (update response :trace/response conj 'response1))

(defn- response2 [response request]
  (println 'response2 response request)
  (update response :trace/response conj 'response2))

;; Register functions to be used in configuration.
(config/as-wrap-response `response1 (constantly response1) {:tags [response1 ::response1]})
(config/as-wrap-response `response2 (constantly response2) {:tags [response2 ::response2]})

;; ## Define handler function to be wrapped

(defn- handler*
  [request]
  (println 'handler* request)
  (assoc request :trace/response []))

;; ## Test composition of wrappers

;; - Compose handler functions

(let [handler (handler/build handler* {:outer [wrap1 wrap2]
                                       :enter [request1 request2]
                                       :leave [response1 response2]
                                       :inner [wrap3 wrap4]})]
  (handler {:trace/request []}))

;wrap1 #:trace{:request []}
;wrap2 #:trace{:request [wrap1]}
;request1 #:trace{:request [wrap1 wrap2]}
;request2 #:trace{:request [wrap1 wrap2 request1]}
;wrap3 #:trace{:request [wrap1 wrap2 request1 request2]}
;wrap4 #:trace{:request [wrap1 wrap2 request1 request2 wrap3]}
;handler* #:trace{:request [wrap1 wrap2 request1 request2 wrap3 wrap4]}
;wrap4 #:trace{:request [wrap1 wrap2 request1 request2 wrap3 wrap4], :response [wrap4]}
;wrap3 #:trace{:request [wrap1 wrap2 request1 request2 wrap3 wrap4], :response [wrap4 wrap3]}
;response1 #:trace{:request [wrap1 wrap2 request1 request2 wrap3 wrap4], :response [wrap4 wrap3]} #:trace{:request [wrap1 wrap2 request1 request2]}
;response2 #:trace{:request [wrap1 wrap2 request1 request2 wrap3 wrap4], :response [wrap4 wrap3 response1]} #:trace{:request [wrap1 wrap2 request1 request2]}
;wrap2 #:trace{:request [wrap1 wrap2 request1 request2 wrap3 wrap4], :response [wrap4 wrap3 response1 response2 wrap2]}
;wrap1 #:trace{:request [wrap1 wrap2 request1 request2 wrap3 wrap4], :response [wrap4 wrap3 response1 response2 wrap2 wrap1]}

#_#:trace{:request [wrap1 wrap2 request1 request2 wrap3 wrap4],
          :response [wrap4 wrap3 response1 response2 wrap2 wrap1]}

;; - Compose handler types (symbols)

(let [handler (handler/build handler* {:outer [`wrap1 `wrap2]
                                       :enter [`request1 `request2]
                                       :leave [`response1 `response2]
                                       :inner [`wrap3 `wrap4]})]
  (handler {:trace/request []}))

;; - Compose handler types (keywords)

(let [handler (handler/build handler* {:outer [::wrap1 ::wrap2]
                                       :enter [::request1 ::request2]
                                       :leave [::response1 ::response2]
                                       :inner [::wrap3 ::wrap4]})]
  (handler {:trace/request []}))

;; - Configuration with dependency

;; Requires to :enter `request1` before `request2`
(config/set-required `request2 {:enter [`request1]})

(comment
  ;; Missing required
  (let [handler (handler/build handler* {:enter [request2]})]
    (handler {:trace/request []}))
  ;clojure.lang.ExceptionInfo:
  ; Missing required: {:enter usage.walkthrough$request2, :required usage.walkthrough/request1}
  ; {:type usage.walkthrough$request2,
  ;  :required {:enter [usage.walkthrough/request1]},
  ;  :missing usage.walkthrough/request1}

  ;; Required in wrong position
  (let [handler (handler/build handler* {:enter [request2
                                                 request1]})]
    (handler {:trace/request []}))
  ;clojure.lang.ExceptionInfo:
  ; Required in wrong position: {:enter usage.walkthrough$request2, :required usage.walkthrough/request1}
  ; {:type usage.walkthrough$request2,
  ;  :required {:enter [usage.walkthrough/request1]},
  ;  :missing usage.walkthrough/request1}

  ;; Ignore dependency error
  (let [handler (handler/build handler* {:enter [request2]
                                         :ignore-required #{request1}})]
    (handler {:trace/request []}))
  ;request2 #:trace{:request []}
  ;handler* #:trace{:request [request2]}
  ;=> #:trace{:request [request2], :response []}
  )
