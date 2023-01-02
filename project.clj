(defproject com.github.strojure/ring-control "1.0.16-SNAPSHOT"
  :description "More controllable composition of Ring middlewares."
  :url "https://github.com/strojure/ring-control"
  :license {:name "The MIT License" :url "http://opensource.org/licenses/MIT"}

  :dependencies []

  :profiles {:provided {:dependencies [[org.clojure/clojure "1.11.1"]
                                       ;; ring middlewares to provide configs for
                                       [ring/ring-core "1.9.6"]
                                       [ring/ring-devel "1.9.6"]
                                       [ring/ring-headers "0.3.0"]
                                       [ring/ring-ssl "0.3.0"]
                                       ;; required for `ring.middleware.multipart-params`
                                       [javax.servlet/javax.servlet-api "4.0.1"]]}
             :dev,,,,, {:source-paths ["doc"]}}

  :deploy-repositories [["clojars" {:url "https://clojars.org/repo" :sign-releases false}]])
