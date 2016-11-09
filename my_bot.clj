; Just testing some ideas
(:require [com.amazing.plugins.amazing-plugin :as amazing-plugin])

(defn my-plugin
  {
   :owner "John Doe <someone@example.com>"
   :function (fn [packet] (println packet))
   })

(def options
  {
  :nickname "EmptyBot"
  :realname "Empty"
  :channel "#emptytest"
  :server "irc.freenode.com"
  ; license and registration.
  :plugins [ (amazing-plugin/registration) my-plugin ]
  })

(rocks.empty.clojure.irc/bot options)
