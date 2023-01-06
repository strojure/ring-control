(ns strojure.ring-control.config.ring-middleware-test
  (:refer-clojure :exclude [test])
  (:require [clojure.test :as test :refer [deftest testing]]
            [strojure.ring-control.config.ring-middleware :as ring]
            [strojure.ring-control.handler :as handler]))

(set! *warn-on-reflection* true)

(declare thrown? thrown-with-msg?)

#_(test/run-tests)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn- test
  ([config request] (test config request {}))
  ([config request response]
   (let [handler (-> (fn [request] (assoc response :request request))
                     (handler/build config))]
     (handler request))))

(defn- test-async
  ([config request] (test config request {}))
  ([config request response]
   (let [handler (-> ^:async (fn [request respond _]
                               (respond (assoc response :request request)))
                     (handler/build config))
         response! (promise)]
     (handler request
              (fn [response] (deliver response! response))
              (fn [e] (throw e)))
     @response!)))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest request-params-t
  (testing "`params` without options"
    (test/are [expr]
              (= "1" (-> expr :request :params (get "a")))

      (test [(ring/req-params)]
            {:query-string "a=1"})

      (test-async [(ring/req-params)]
                  {:query-string "a=1"})

      )))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest request-keyword-params-t

  (testing "`keyword-params` without options"
    (test/are [expr]
              (= "1" (-> expr :request :params :a))

      (test [(ring/req-params)
             (ring/req-keyword-params)]
            {:query-string "a=1"})

      (test-async [(ring/req-params)
                   (ring/req-keyword-params)]
                  {:query-string "a=1"})

      ))
  (testing "`keyword-params` with `:parse-namespaces?` true"
    (test/are [expr]
              (= "1" (-> expr :request :params :ns/a))

      (test [(ring/req-params)
             (ring/req-keyword-params :parse-namespaces? true)]
            {:query-string "ns/a=1"})

      (test [(ring/req-params)
             (ring/req-keyword-params {:parse-namespaces? true})]
            {:query-string "ns/a=1"})

      ))
  )

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest request-multipart-params-t
  (test/are [expr]
            (-> expr :request (contains? :multipart-params))

    (test [(ring/req-multipart-params)]
          {})

    ))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest request-nested-params-t
  (test/are [expr]
            (= ["bar"] (-> expr :request :params (get "foo")))

    (test [(ring/req-params)
           (ring/req-nested-params)]
          {:query-string "foo[]=bar"})

    ))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest response-content-type-t
  (testing "`content-type-response` without options"
    (test/are [expr]
              (= "text/html" (-> expr :headers (get "Content-Type")))

      (test [(ring/resp-content-type)]
            {:uri "/index.html"})

      )))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest wrap-cookies-t
  (test/are [expr] expr

    (-> (test [(ring/wrap-cookies)] {})
        :request
        (contains? :cookies))

    (-> (test [(ring/wrap-cookies)] {} {:cookies {:a 1}})
        :headers
        (get "Set-Cookie")
        (first)
        #{"a=1"})

    ))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest wrap-session-t
  (test/are [expr] expr

    (-> (test [(ring/wrap-session)] {})
        :request
        (contains? :session))

    (-> (test [(ring/wrap-session false)] {})
        :request
        (contains? :session)
        (not))

    (-> (test [(ring/wrap-session)] {} {:session {:a 1}})
        :headers
        (contains? "Set-Cookie"))

    ))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest wrap-flash-t
  (test/are [expr] (-> expr :request (contains? :flash))

    (test [(ring/wrap-session)
           (ring/wrap-flash)]
          {})

    ))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
