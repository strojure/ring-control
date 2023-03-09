(ns benchmarks.handler
  (:require [strojure.ring-control.handler :as handler]))

(set! *warn-on-reflection* true)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn- request-fn [k]
  (fn [request] (assoc request k 0))
  #_identity)

(def ^:private -h1
  (handler/build identity [{:enter (request-fn :a)}
                           {:enter (request-fn :b)}
                           {:enter (request-fn :c)}
                           {:enter (request-fn :d)}
                           {:enter (request-fn :e)}]))

(defn- handler-fn [k]
  (let [request-fn* (request-fn k)]
    (fn [handler]
      (fn
        ([request]
         (handler (request-fn* request)))
        ([request respond raise]
         (handler (request-fn* request) respond raise))))))

(def ^:private -h2
  (handler/build identity [{:wrap (handler-fn :a)}
                           {:wrap (handler-fn :b)}
                           {:wrap (handler-fn :c)}
                           {:wrap (handler-fn :d)}
                           {:wrap (handler-fn :e)}]))

(-h1 {})
(-h2 {})

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
