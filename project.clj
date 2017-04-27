(defproject otomatik "0.2.0-SNAPSHOT"
  :description "A simple IRC Bot Framework written in Clojure."
  :url "https://github.com/TheEmpty/Otomatik"
  :license {:name "MIT License"
            :url "http://www.opensource.org/licenses/mit-license.php"}
  :dependencies [
    [org.clojure/clojure "1.8.0"]
    [org.clojure/core.async "0.2.395"]
    [org.clojure/tools.logging "0.3.1"]
  ]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
