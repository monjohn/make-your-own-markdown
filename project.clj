(defproject make-your-own-markdown "0.1.0"
  :description "Utilities for generating the German for Reading site"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-beta4"]
                 [hiccup "2.0.0-alpha1"]
                 [instaparse "1.4.8"]
                 [fs "1.3.3"]
                 [me.raynes/fs "1.4.6"]]
  :main ^:skip-aot make-your-own-markdown.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
