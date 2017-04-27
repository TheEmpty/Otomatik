(ns rocks.empty.otomatik.default-plugins.channel-joiner
    (:require [clojure.tools.logging :as log]))

(defn registration
  [irc-channels]
  {
    :name "Channel Joiner"
    :author "@the_empty on GitHub"
    :otomatik_version 0.2

    :on-connect (fn [write-fn]
      (log/debug "Joining " irc-channels)
      (doall (map #(write-fn (str "JOIN " %1)) irc-channels)))
  })
