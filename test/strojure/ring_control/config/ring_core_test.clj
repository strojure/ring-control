(ns strojure.ring-control.config.ring-core-test
  (:refer-clojure :exclude [test])
  (:require [clojure.test :as test :refer [deftest testing]]
            [strojure.ring-control.config.ring-core :as ring]
            [strojure.ring-control.handler :as handler]))

(set! *warn-on-reflection* true)

(declare thrown? thrown-with-msg?)

#_(test/run-tests)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn- test [config]
  (let [handler (-> (fn [request] {:request request
                                   :cookies {:a 1}
                                   :session {:sess/a 1}
                                   :flash ::flash})
                    (handler/build config))]
    (handler {:uri "/index.html"
              :query-string "a=1&ns/b=2&foo[]=bar"})))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest params-request-t
  (testing "`params` without options"
    (test/are [expr]
              (= "1" (-> expr :request :params (get "a")))

      (test {:enter [(ring/params-request)]})
      (test {:enter [ring/params-request]})
      (test {:enter [`ring/params-request]})
      (test {:enter [::ring/params-request]})
      (test {:enter [{:type ring/params-request}]})
      (test {:enter [{:type `ring/params-request}]})
      (test {:enter [{:type ::ring/params-request}]})

      ))
  (testing "`params` with :encoding"
    (test/are [expr]
              (= "1" (-> expr :request :params (get "a")))

      (test {:enter [(ring/params-request {:encoding "UTF-8"})]})
      (test {:enter [(ring/params-request :encoding "UTF-8")]})
      (test {:enter [{:type ring/params-request :encoding "UTF-8"}]})
      (test {:enter [{:type `ring/params-request :encoding "UTF-8"}]})
      (test {:enter [{:type ::ring/params-request :encoding "UTF-8"}]})

      ))
  (testing "`params` in wrong group"
    (test/are [expr]
              (thrown? Exception expr)

      (test {:leave [(ring/params-request)]})
      (test {:leave [ring/params-request]})
      (test {:leave [`ring/params-request]})
      (test {:leave [::ring/params-request]})
      (test {:outer [(ring/params-request)]})
      (test {:outer [ring/params-request]})
      (test {:outer [`ring/params-request]})
      (test {:outer [::ring/params-request]})

      )))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest keyword-params-request-t

  (testing "`keyword-params` without options"
    (test/are [expr]
              (= "1" (-> expr :request :params :a))

      (test {:enter [(ring/params-request)
                     (ring/keyword-params-request)]})

      (test {:enter [ring/params-request
                     ring/keyword-params-request]})

      (test {:enter [`ring/params-request
                     `ring/keyword-params-request]})

      (test {:enter [::ring/params-request
                     ::ring/keyword-params-request]})

      (test {:enter [{:type ring/params-request}
                     {:type ring/keyword-params-request}]})

      (test {:enter [{:type `ring/params-request}
                     {:type `ring/keyword-params-request}]})

      (test {:enter [{:type ::ring/params-request}
                     {:type ::ring/keyword-params-request}]})

      ))

  (testing "`keyword-params` with `:parse-namespaces?` true"
    (test/are [expr]
              (= "2" (-> expr :request :params :ns/b))

      (test {:enter [(ring/params-request)
                     (ring/keyword-params-request {:parse-namespaces? true})]})

      (test {:enter [(ring/params-request)
                     (ring/keyword-params-request :parse-namespaces? true)]})

      (test {:enter [{:type ring/params-request}
                     {:type ring/keyword-params-request :parse-namespaces? true}]})

      (test {:enter [{:type `ring/params-request}
                     {:type `ring/keyword-params-request :parse-namespaces? true}]})

      (test {:enter [{:type ::ring/params-request}
                     {:type ::ring/keyword-params-request :parse-namespaces? true}]})

      ))
  )

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest multipart-params-request-t
  (test/are [expr]
            (-> expr :request (contains? :multipart-params))

    (test {:enter [(ring/multipart-params-request)]})

    ))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest nested-params-request-t
  (test/are [expr]
            (= ["bar"] (-> expr :request :params (get "foo")))

    (test {:enter [(ring/params-request)
                   (ring/nested-params-request)]})

    ))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest content-type-response-t
  (testing "`content-type-response` without options"
    (test/are [expr]
              (= "text/html" (-> expr :headers (get "Content-Type")))

      (test {:leave [(ring/content-type-response)]})
      (test {:leave [ring/content-type-response]})
      (test {:leave [`ring/content-type-response]})
      (test {:leave [::ring/content-type-response]})
      (test {:leave [{:type ring/content-type-response}]})
      (test {:leave [{:type `ring/content-type-response}]})
      (test {:leave [{:type ::ring/content-type-response}]})

      )))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest wrap-cookies-t
  (test/are [expr] expr

    (-> (test {:outer [ring/cookies-handler]})
        :request
        (contains? :cookies))

    (-> (test {:outer [ring/cookies-handler]})
        :headers
        (get "Set-Cookie")
        (first)
        #{"a=1"})

    ))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest wrap-session-t
  (test/are [expr] expr

    (-> (test {:outer [ring/session-handler]})
        :request
        (contains? :session))

    (-> (test {:outer [ring/session-handler]})
        :headers
        (contains? "Set-Cookie"))

    ))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest wrap-flash-t
  (test/are [expr] (-> expr :request (contains? :flash))

    (test {:outer [ring/session-handler
                   ring/flash-handler]})

    (test {:inner [ring/session-handler
                   ring/flash-handler]})

    (test {:outer [ring/session-handler]
           :inner [ring/flash-handler]})

    )
  (test/are [expr] (thrown-with-msg? Exception #"(?i)missing" expr)

    (test {:outer [ring/flash-handler]})
    (test {:inner [ring/flash-handler]})

    )
  (test/are [expr] (thrown-with-msg? Exception #"(?i)misplaced" expr)

    (test {:outer [ring/flash-handler
                   ring/session-handler]})

    (test {:inner [ring/flash-handler
                   ring/session-handler]})

    (test {:outer [ring/flash-handler]
           :inner [ring/session-handler]})

    ))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
