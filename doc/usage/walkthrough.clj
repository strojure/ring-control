(ns usage.walkthrough
  (:require [strojure.ring-control.handler :as handler]))

;; ## Define builder configuration

;; - Define standard ring handlers

(defn- wrap1 [handler]
  (fn [request]
    (println :enter 'wrap1 {:request request})
    (doto (-> (update request :trace/enter conj 'wrap1)
              (handler)
              (update :trace/leave conj 'wrap1))
      (as-> response (println :leave 'wrap1 {:response response})))))

(defn- wrap2 [handler]
  (fn [request]
    (println :enter 'wrap2 {:request request})
    (doto (-> (update request :trace/enter conj 'wrap2)
              (handler)
              (update :trace/leave conj 'wrap2))
      (as-> response (println :leave 'wrap2 {:response response})))))

(defn- wrap3 [handler]
  (fn [request]
    (println :enter 'wrap3 {:request request})
    (doto (-> (update request :trace/enter conj 'wrap3)
              (handler)
              (update :trace/leave conj 'wrap3))
      (as-> response (println :leave 'wrap3 {:response response})))))

(defn- wrap4 [handler]
  (fn [request]
    (println :enter 'wrap4 {:request request})
    (doto (-> (update request :trace/enter conj 'wrap4)
              (handler)
              (update :trace/leave conj 'wrap4))
      (as-> response (println :leave 'wrap4 {:response response})))))

;; - Define ring request wrappers

(defn- request1 [request]
  (println :enter 'request1 {:request request})
  (update request :trace/enter conj 'request1))

(defn- request2 [request]
  (println :enter 'request2 {:request request})
  (update request :trace/enter conj 'request2))

;; - Define ring response wrappers

(defn- response1 [response request]
  (println :leave 'response1 {:response response :request request})
  (update response :trace/leave conj 'response1))

(defn- response2 [response request]
  (println :leave 'response2 {:response response :request request})
  (update response :trace/leave conj 'response2))

;; ## Define handler function to be wrapped

(defn- handler*
  [request]
  (println :handler {:request request})
  (assoc request :trace/leave []))

;; ## Test composition of wrappers

;; - :enter before :leave

(let [handler (handler/build handler* [{:wrap wrap1}
                                       {:wrap wrap2}
                                       {:enter request1}
                                       {:enter request2}
                                       {:leave response1}
                                       {:leave response2}
                                       {:wrap wrap3}
                                       {:wrap wrap4}])]
  (handler {:trace/enter []}))

;:enter wrap1 {:request #:trace{:enter []}}
;:enter wrap2 {:request #:trace{:enter [wrap1]}}
;:enter request1 {:request #:trace{:enter [wrap1 wrap2]}}
;:enter request2 {:request #:trace{:enter [wrap1 wrap2 request1]}}
;:enter wrap3 {:request #:trace{:enter [wrap1 wrap2 request1 request2]}}
;:enter wrap4 {:request #:trace{:enter [wrap1 wrap2 request1 request2 wrap3]}}
;:handler {:request #:trace{:enter [wrap1 wrap2 request1 request2 wrap3 wrap4]}}
;:leave wrap4 {:response #:trace{:enter [wrap1 wrap2 request1 request2 wrap3 wrap4], :leave [wrap4]}}
;:leave wrap3 {:response #:trace{:enter [wrap1 wrap2 request1 request2 wrap3 wrap4], :leave [wrap4 wrap3]}}
;:leave response2 {:response #:trace{:enter [wrap1 wrap2 request1 request2 wrap3 wrap4], :leave [wrap4 wrap3]}, :request #:trace{:enter [wrap1 wrap2 request1 request2]}}
;:leave response1 {:response #:trace{:enter [wrap1 wrap2 request1 request2 wrap3 wrap4], :leave [wrap4 wrap3 response2]}, :request #:trace{:enter [wrap1 wrap2 request1 request2]}}
;:leave wrap2 {:response #:trace{:enter [wrap1 wrap2 request1 request2 wrap3 wrap4], :leave [wrap4 wrap3 response2 response1 wrap2]}}
;:leave wrap1 {:response #:trace{:enter [wrap1 wrap2 request1 request2 wrap3 wrap4], :leave [wrap4 wrap3 response2 response1 wrap2 wrap1]}}

#_#:trace{:enter [wrap1 wrap2 request1 request2 wrap3 wrap4],
          :leave [wrap4 wrap3 response2 response1 wrap2 wrap1]}

;; - :leave before :enter

(let [handler (handler/build handler* [{:wrap wrap1}
                                       {:wrap wrap2}
                                       {:leave response1}
                                       {:leave response2}
                                       {:enter request1}
                                       {:enter request2}
                                       {:wrap wrap3}
                                       {:wrap wrap4}])]
  (handler {:trace/enter []}))

;:enter wrap1 {:request #:trace{:enter []}}
;:enter wrap2 {:request #:trace{:enter [wrap1]}}
;:enter request1 {:request #:trace{:enter [wrap1 wrap2]}}
;:enter request2 {:request #:trace{:enter [wrap1 wrap2 request1]}}
;:enter wrap3 {:request #:trace{:enter [wrap1 wrap2 request1 request2]}}
;:enter wrap4 {:request #:trace{:enter [wrap1 wrap2 request1 request2 wrap3]}}
;:handler {:request #:trace{:enter [wrap1 wrap2 request1 request2 wrap3 wrap4]}}
;:leave wrap4 {:response #:trace{:enter [wrap1 wrap2 request1 request2 wrap3 wrap4], :leave [wrap4]}}
;:leave wrap3 {:response #:trace{:enter [wrap1 wrap2 request1 request2 wrap3 wrap4], :leave [wrap4 wrap3]}}
;:leave response2 {:response #:trace{:enter [wrap1 wrap2 request1 request2 wrap3 wrap4], :leave [wrap4 wrap3]}, :request #:trace{:enter [wrap1 wrap2]}}
;:leave response1 {:response #:trace{:enter [wrap1 wrap2 request1 request2 wrap3 wrap4], :leave [wrap4 wrap3 response2]}, :request #:trace{:enter [wrap1 wrap2]}}
;:leave wrap2 {:response #:trace{:enter [wrap1 wrap2 request1 request2 wrap3 wrap4], :leave [wrap4 wrap3 response2 response1 wrap2]}}
;:leave wrap1 {:response #:trace{:enter [wrap1 wrap2 request1 request2 wrap3 wrap4], :leave [wrap4 wrap3 response2 response1 wrap2 wrap1]}}

#_#:trace{:enter [wrap1 wrap2 request1 request2 wrap3 wrap4],
          :leave [wrap4 wrap3 response2 response1 wrap2 wrap1]}
