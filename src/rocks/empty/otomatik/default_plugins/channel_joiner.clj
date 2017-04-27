(ns rocks.empty.otomatik.default-plugins.channel-joiner
    (:require [clojure.tools.logging :as log]))

(defn registration
  [irc-channels]
  {
    :author "@the_empty on GitHub"
    :init (fn [args]
      (log/debug "Joining " irc-channels)
      (map #(str "JOIN " %1) irc-channels))
  })
