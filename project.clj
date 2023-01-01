(defproject com.github.strojure/ring-control "1.0.16-SNAPSHOT"
  :description "More controllable composition of Ring middlewares."
  :url "https://github.com/strojure/ring-control"
  :license {:name "The MIT License" :url "http://opensource.org/licenses/MIT"}

  :dependencies [[ring/ring-core "1.9.6"]]

  :profiles {:provided {:dependencies [[org.clojure/clojure "1.11.1"]
                                       ;; required for `ring.middleware.multipart-params`
                                       [javax.servlet/javax.servlet-api "4.0.1"]]}
             :dev,,,,, {:source-paths ["doc"]}}

  :deploy-repositories [["clojars" {:url "https://clojars.org/repo" :sign-releases false}]])
