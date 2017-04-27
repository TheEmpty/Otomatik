(ns rocks.empty.otomatik.channel-plugin-writer
  (:require [clojure.tools.logging :as log])
  (:require [clojure.core.async :as a :refer [go >!]]))

; Provides a fn that allows plugins to write
; to a channel without having direct access to it.

(defn create
  [chan]
  (fn [message]
    (log/trace "Adding:" message)
    (go (>! chan message))))
