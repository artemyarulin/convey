(defproject convey "1.0.1"
  :description "Reactive programming implemented using transducers"
  :url "https://github.com/artemyarulin/convey"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.48"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]]
  :plugins [[lein-cljsbuild "1.0.6"]
            [com.cemerick/clojurescript.test "0.3.3"]]
  :cljsbuild {:builds {:main {:source-paths ["src"]}}})
