(ns strojure.ring-builder.middleware.ring-core-test
  (:refer-clojure :exclude [test])
  (:require [clojure.test :as test :refer [deftest testing]]
            [strojure.ring-builder.handler :as handler]
            [strojure.ring-builder.middleware.ring-core :as ring]))

(set! *warn-on-reflection* true)

(declare thrown? thrown-with-msg?)

#_(test/run-tests)

;;,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

(defn- test [config]
  (let [handler (-> (fn [request] {:request request})
                    (handler/build config))]
    (handler {:uri "/" :query-string "a=1&ns/b=2"})))

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

  (testing "`keyword-params` required in wrong position"
    (test/are [expr]
              (thrown-with-msg? Exception #"(?i)wrong" expr)

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
