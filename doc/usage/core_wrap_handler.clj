(ns usage.core-wrap-handler
  (:require [strojure.ring-stack.core :as stack])
  (:import (clojure.lang MultiFn)))

;; ## Define middleware functions

;; - Define standard ring handlers

(defn- wrap-outer-1 [handler]
  (fn [request]
    (println 'wrap-outer-1 request)
    (doto (-> (update request :trace/request conj 'wrap-outer-1)
              (handler)
              (update :trace/response conj 'wrap-outer-1))
      (->> (println 'wrap-outer-1)))))

(defn- wrap-outer-2 [handler]
  (fn [request]
    (println 'wrap-outer-2 request)
    (doto (-> (update request :trace/request conj 'wrap-outer-2)
              (handler)
              (update :trace/response conj 'wrap-outer-2))
      (->> (println 'wrap-outer-2)))))

(defn- wrap-inner-1 [handler]
  (fn [request]
    (println 'wrap-inner-1 request)
    (doto (-> (update request :trace/request conj 'wrap-inner-1)
              (handler)
              (update :trace/response conj 'wrap-inner-1))
      (->> (println 'wrap-inner-1)))))

(defn- wrap-inner-2 [handler]
  (fn [request]
    (println 'wrap-inner-2 request)
    (doto (-> (update request :trace/request conj 'wrap-inner-2)
              (handler)
              (update :trace/response conj 'wrap-inner-2))
      (->> (println 'wrap-inner-2)))))

;; - Define ring request wrappers

(defn- wrap-request-1 [request]
  (println 'wrap-request-1 request)
  (update request :trace/request conj 'wrap-request-1))

(defn- wrap-request-2 [request]
  (println 'wrap-request-2 request)
  (update request :trace/request conj 'wrap-request-2))

;; - Define ring response wrappers

(defn- wrap-response-1 [response request]
  (println 'wrap-response-1 response request)
  (update response :trace/response conj 'wrap-response-1))

(defn- wrap-response-2 [response request]
  (println 'wrap-response-2 response request)
  (update response :trace/response conj 'wrap-response-2))

;; ## Define handler function to be wrapped

(defn- handler*
  [request]
  (println 'handler* request)
  (assoc request :trace/response []))

;; ## Test composition of wrappers

;; - Compose handler functions

(let [handler (stack/wrap-handler handler* {:outer [wrap-outer-1
                                                    wrap-outer-2]
                                            :enter [wrap-request-1
                                                    wrap-request-2]
                                            :leave [wrap-response-1
                                                    wrap-response-2]
                                            :inner [wrap-inner-1
                                                    wrap-inner-2]})]
  (handler {:trace/request []}))

;wrap-outer-1 #:trace{:request []}
;wrap-outer-2 #:trace{:request [wrap-outer-1]}
;wrap-request-1 #:trace{:request [wrap-outer-1 wrap-outer-2]}
;wrap-request-2 #:trace{:request [wrap-outer-1 wrap-outer-2 wrap-request-1]}
;wrap-inner-1 #:trace{:request [wrap-outer-1 wrap-outer-2 wrap-request-1 wrap-request-2]}
;wrap-inner-2 #:trace{:request [wrap-outer-1 wrap-outer-2 wrap-request-1 wrap-request-2 wrap-inner-1]}
;handler* #:trace{:request [wrap-outer-1 wrap-outer-2 wrap-request-1 wrap-request-2 wrap-inner-1 wrap-inner-2]}
;wrap-inner-2 #:trace{:request [wrap-outer-1 wrap-outer-2 wrap-request-1 wrap-request-2 wrap-inner-1 wrap-inner-2], :response [wrap-inner-2]}
;wrap-inner-1 #:trace{:request [wrap-outer-1 wrap-outer-2 wrap-request-1 wrap-request-2 wrap-inner-1 wrap-inner-2], :response [wrap-inner-2 wrap-inner-1]}
;wrap-response-1 #:trace{:request [wrap-outer-1 wrap-outer-2 wrap-request-1 wrap-request-2 wrap-inner-1 wrap-inner-2], :response [wrap-inner-2 wrap-inner-1]} #:trace{:request [wrap-outer-1 wrap-outer-2 wrap-request-1 wrap-request-2]}
;wrap-response-2 #:trace{:request [wrap-outer-1 wrap-outer-2 wrap-request-1 wrap-request-2 wrap-inner-1 wrap-inner-2], :response [wrap-inner-2 wrap-inner-1 wrap-response-1]} #:trace{:request [wrap-outer-1 wrap-outer-2 wrap-request-1 wrap-request-2]}
;wrap-outer-2 #:trace{:request [wrap-outer-1 wrap-outer-2 wrap-request-1 wrap-request-2 wrap-inner-1 wrap-inner-2], :response [wrap-inner-2 wrap-inner-1 wrap-response-1 wrap-response-2 wrap-outer-2]}
;wrap-outer-1 #:trace{:request [wrap-outer-1 wrap-outer-2 wrap-request-1 wrap-request-2 wrap-inner-1 wrap-inner-2], :response [wrap-inner-2 wrap-inner-1 wrap-response-1 wrap-response-2 wrap-outer-2 wrap-outer-1]}

#_#:trace{:request [wrap-outer-1 wrap-outer-2 wrap-request-1 wrap-request-2 wrap-inner-1 wrap-inner-2],
          :response [wrap-inner-2 wrap-inner-1 wrap-response-1 wrap-response-2 wrap-outer-2 wrap-outer-1]}

;; - Compose registered handler types

;; Assign middleware types to our middlewares
(.addMethod ^MultiFn stack/as-handler-wrap ::wrap-outer-1 (constantly wrap-outer-1))
(.addMethod ^MultiFn stack/as-handler-wrap ::wrap-outer-2 (constantly wrap-outer-2))
(.addMethod ^MultiFn stack/as-handler-wrap ::wrap-inner-1 (constantly wrap-inner-1))
(.addMethod ^MultiFn stack/as-handler-wrap ::wrap-inner-2 (constantly wrap-inner-2))
(.addMethod ^MultiFn stack/as-request-wrap ::wrap-request-1 (constantly wrap-request-1))
(.addMethod ^MultiFn stack/as-request-wrap ::wrap-request-2 (constantly wrap-request-2))
(.addMethod ^MultiFn stack/as-response-wrap ::wrap-response-1 (constantly wrap-response-1))
(.addMethod ^MultiFn stack/as-response-wrap ::wrap-response-2 (constantly wrap-response-2))

(let [handler (stack/wrap-handler handler* {:outer [::wrap-outer-1
                                                    ::wrap-outer-2]
                                            :enter [::wrap-request-1
                                                    ::wrap-request-2]
                                            :leave [::wrap-response-1
                                                    ::wrap-response-2]
                                            :inner [::wrap-inner-1
                                                    ::wrap-inner-2]})]
  (handler {:trace/request []}))

;; - Compose middlewares with dependencies

;; Requires to :enter ::wrap-request-1 before ::wrap-request-2
(.addMethod ^MultiFn stack/require-config ::wrap-request-2
            (constantly {:enter [::wrap-request-1]}))

(comment
  ;; Missing required middleware
  (let [handler (stack/wrap-handler handler* {:enter [::wrap-request-2]})]
    (handler {:trace/request []}))
  ;clojure.lang.ExceptionInfo:
  ; Missing required middleware: {:middleware :usage.core/wrap-request-2, :requires :usage.core/wrap-request-1}
  ; {:middleware-type :usage.core/wrap-request-2,
  ;  :require-config {:enter [:usage.core/wrap-request-1]},
  ;  :middleware :usage.core/wrap-request-2,
  ;  :missing :usage.core/wrap-request-1}

  ;; Required middleware is in wrong position
  (let [handler (stack/wrap-handler handler* {:enter [::wrap-request-2
                                                      ::wrap-request-1]})]
    (handler {:trace/request []}))
  ;clojure.lang.ExceptionInfo:
  ; Required middleware is in wrong position: {:middleware :usage.core/wrap-request-2, :requires :usage.core/wrap-request-1}
  ; {:middleware-type :usage.core/wrap-request-2,
  ;  :require-config {:enter [:usage.core/wrap-request-1]},
  ;  :middleware :usage.core/wrap-request-2,
  ;  :missing :usage.core/wrap-request-1}

  ;; Ignore dependency error
  (let [handler (stack/wrap-handler handler* {:enter [::wrap-request-2]
                                              :ignore-required #{::wrap-request-1}})]
    (handler {:trace/request []}))
  ;wrap-request-2 #:trace{:request []}
  ;handler* #:trace{:request [wrap-request-2]}
  ;=> #:trace{:request [wrap-request-2], :response []}
  )
