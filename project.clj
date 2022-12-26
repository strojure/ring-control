(defproject com.github.strojure/ring-stack "1.0.3-beta1"
  :description "More controllable composition of Ring middlewares."
  :url "https://github.com/strojure/ring-stack"
  :license {:name "The MIT License" :url "http://opensource.org/licenses/MIT"}

  :dependencies [[ring/ring-core "1.9.6"]]

  :profiles {:provided {:dependencies [[org.clojure/clojure "1.11.1"]]}
             :dev,,,,, {:source-paths ["doc"]}}

  :deploy-repositories [["clojars" {:url "https://clojars.org/repo" :sign-releases false}]])
