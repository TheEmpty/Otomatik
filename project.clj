(defproject otomatik "0.2.0-SNAPSHOT"
  :description "A simple IRC Bot Framework written in Clojure."
  :url "https://github.com/TheEmpty/Otomatik"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
    [org.clojure/clojure "1.8.0"]
    [org.clojure/core.async "0.2.395"]
  ]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
