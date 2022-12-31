(ns strojure.ring-control.middleware.ring-core-test
  (:refer-clojure :exclude [test])
  (:require [clojure.test :as test :refer [deftest testing]]
            [strojure.ring-control.handler :as handler]
            [strojure.ring-control.middleware.ring-core :as ring]))

(set! *warn-on-reflection* true)

(declare thrown? thrown-with-msg?)

#_(test/run-tests)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn- test [config]
  (let [handler (-> (fn [request] {:request request
                                   :session {:sess/a 1}
                                   :flash ::flash})
                    (handler/build config))]
    (handler {:uri "/index.html"
              :query-string "a=1&ns/b=2"})))

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

  (testing "`keyword-params` missing required"
    (test/are [expr]
              (thrown-with-msg? Exception #"(?i)missing" expr)

      (test {:enter [(ring/keyword-params-request)]})

      (test {:enter [ring/keyword-params-request]})

      (test {:enter [`ring/keyword-params-request]})

      (test {:enter [::ring/keyword-params-request]})

      ))

  (testing "`keyword-params` misplaced required"
    (test/are [expr]
              (thrown-with-msg? Exception #"(?i)misplaced" expr)

      (test {:enter [(ring/keyword-params-request)
                     (ring/params-request)]})

      (test {:enter [ring/keyword-params-request
                     ring/params-request]})

      (test {:enter [`ring/keyword-params-request
                     `ring/params-request]})

      (test {:enter [::ring/keyword-params-request
                     ::ring/params-request]})

      )))

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

(deftest wrap-session-t
  (test/are [expr] expr

    (-> (test {:outer [ring/wrap-session]})
        :request
        (contains? :session))

    (-> (test {:outer [ring/wrap-session]})
        :headers
        (contains? "Set-Cookie"))

    ))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(deftest wrap-flash-t
  (test/are [expr] (-> expr :request (contains? :flash))

    (test {:outer [ring/wrap-session
                   ring/wrap-flash]})

    (test {:inner [ring/wrap-session
                   ring/wrap-flash]})

    (test {:outer [ring/wrap-session]
           :inner [ring/wrap-flash]})

    )
  (test/are [expr] (thrown-with-msg? Exception #"(?i)missing" expr)

    (test {:outer [ring/wrap-flash]})
    (test {:inner [ring/wrap-flash]})

    )
  (test/are [expr] (thrown-with-msg? Exception #"(?i)misplaced" expr)

    (test {:outer [ring/wrap-flash
                   ring/wrap-session]})

    (test {:inner [ring/wrap-flash
                   ring/wrap-session]})

    (test {:outer [ring/wrap-flash]
           :inner [ring/wrap-session]})

    ))

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,
