(ns rocks.empty.otomatik.default-plugins.channel-joiner
  (:require [clojure.core.async
             :as a
             :refer [>! <! >!! <!! go chan buffer close! thread alts! alts!! timeout]]))

(defn joinChans
  [irc-channels]
  (fn [args]
    (doseq [irc-chan irc-channels] (>!! (:out args) (str "JOIN " irc-chan)))
  ))

(defn registration
  [irc-channels]
  {
    :author "@the_empty on GitHub"
    :init (joinChans irc-channels)
  })
