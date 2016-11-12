(ns rocks.empty.clojure.irc.default-plugins.channel-joiner
  (:require [rocks.empty.clojure.irc.irc-commands :as irc-commands]))

(defn joinChans
  [channels]
  (fn [connection]
    (doseq [chan channels] (irc-commands/irc-command (:writer connection) "JOIN" chan))
  ))

(defn registration
  [channels]
  {
    :author "@the_empty on GitHub"
    :init (joinChans channels)
  })