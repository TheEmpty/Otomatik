; Just testing some ideas
(ns rocks.empty.clojure.irc.test-bot
  (:require [rocks.empty.clojure.irc.irc-client :as irc-client]))

(defn my-plugin
  []
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
  :port 6667
  ; license and registration.
  :plugins [ my-plugin ]
  })

(irc-client/bot options)
(println "Bye!")

