(ns usage.walkthrough2
  (:require [strojure.ring-control.handler :as handler]))

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

;; - Define ring request wrappers

(defn- request1 [request]
  (println 'request1 request)
  (update request :trace/request conj 'request1))

(defn- request2 [request]
  (println 'request2 request)
  (update request :trace/request conj 'request2))

;; - Define ring response wrappers

(defn- response1 [response request]
  (println 'response1 response request)
  (update response :trace/response conj 'response1))

(defn- response2 [response request]
  (println 'response2 response request)
  (update response :trace/response conj 'response2))

;; ## Define handler function to be wrapped

(defn- handler*
  [request]
  (println 'handler* request)
  (assoc request :trace/response []))

;; ## Test composition of wrappers

;; - Compose handler functions

(let [handler (handler/build2 handler* [wrap1
                                        wrap2
                                        (handler/wrap-request [request1 request2])
                                        (handler/wrap-response [response1 response2])
                                        wrap3
                                        wrap4])]
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
