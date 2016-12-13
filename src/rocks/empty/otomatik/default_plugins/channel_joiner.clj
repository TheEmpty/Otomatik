(ns rocks.empty.otomatik.default-plugins.channel-joiner)

(defn registration
  [irc-channels]
  {
    :author "@the_empty on GitHub"
    :init (fn [args] (map #(str "JOIN " %1) irc-channels))
  })
