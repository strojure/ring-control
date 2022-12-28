(ns usage.core-wrap-handler
  (:require [strojure.ring-stack.core :as stack]))

;; ## Define middleware functions

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

(stack/set-as-handler-fn `wrap2 (constantly wrap2))

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
(stack/set-as-handler-fn `wrap1 (constantly wrap1) {:type-aliases [wrap1 ::wrap1]})
(stack/set-as-handler-fn `wrap2 (constantly wrap2) {:type-aliases [wrap2 ::wrap2]})
(stack/set-as-handler-fn `wrap3 (constantly wrap3) {:type-aliases [wrap3 ::wrap3]})
(stack/set-as-handler-fn `wrap4 (constantly wrap4) {:type-aliases [wrap4 ::wrap4]})

;; - Define ring request wrappers

(defn- request1 [request]
  (println 'request1 request)
  (update request :trace/request conj 'request1))

(defn- request2 [request]
  (println 'request2 request)
  (update request :trace/request conj 'request2))

;; Register functions to be used in configuration.
(stack/set-as-request-fn `request1 (constantly request1) {:type-aliases [request1 ::request1]})
(stack/set-as-request-fn `request2 (constantly request2) {:type-aliases [request2 ::request2]})

;; - Define ring response wrappers

(defn- response1 [response request]
  (println 'response1 response request)
  (update response :trace/response conj 'response1))

(defn- response2 [response request]
  (println 'response2 response request)
  (update response :trace/response conj 'response2))

;; Register functions to be used in configuration.
(stack/set-as-response-fn `response1 (constantly response1) {:type-aliases [response1 ::response1]})
(stack/set-as-response-fn `response2 (constantly response2) {:type-aliases [response2 ::response2]})

;; ## Define handler function to be wrapped

(defn- handler*
  [request]
  (println 'handler* request)
  (assoc request :trace/response []))

;; ## Test composition of wrappers

;; - Compose handler functions

(let [handler (stack/wrap-handler handler* {:outer [wrap1 wrap2]
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

(let [handler (stack/wrap-handler handler* {:outer [`wrap1 `wrap2]
                                            :enter [`request1 `request2]
                                            :leave [`response1 `response2]
                                            :inner [`wrap3 `wrap4]})]
  (handler {:trace/request []}))

;; - Compose handler types (keywords)

(let [handler (stack/wrap-handler handler* {:outer [::wrap1 ::wrap2]
                                            :enter [::request1 ::request2]
                                            :leave [::response1 ::response2]
                                            :inner [::wrap3 ::wrap4]})]
  (handler {:trace/request []}))

;; - Middleware with dependency

;; Requires to :enter `request1` before `request2`
(stack/set-required-config `request2 {:enter [`request1]})

(comment
  ;; Missing required middleware
  (let [handler (stack/wrap-handler handler* {:enter [request2]})]
    (handler {:trace/request []}))
  ;clojure.lang.ExceptionInfo:
  ; Missing required middleware: {:middleware usage.core_wrap_handler$request2, :requires usage.core-wrap-handler/request1} {:middleware usage.core_wrap_handler$request2, :required-config {:enter [usage.core-wrap-handler/request1]}, :missing usage.core-wrap-handler/request1}

  ;; Required middleware in wrong position
  (let [handler (stack/wrap-handler handler* {:enter [request2
                                                      request1]})]
    (handler {:trace/request []}))
  ;clojure.lang.ExceptionInfo:
  ; Required middleware in wrong position: {:middleware usage.core_wrap_handler$request2, :requires usage.core-wrap-handler/request1} {:middleware usage.core_wrap_handler$request2, :required-config {:enter [usage.core-wrap-handler/request1]}, :missing usage.core-wrap-handler/request1}

  ;; Ignore dependency error
  (let [handler (stack/wrap-handler handler* {:enter [request2]
                                              :ignore-required #{request1}})]
    (handler {:trace/request []}))
  ;request2 #:trace{:request []}
  ;handler* #:trace{:request [request2]}
  ;=> #:trace{:request [request2], :response []}
  )
