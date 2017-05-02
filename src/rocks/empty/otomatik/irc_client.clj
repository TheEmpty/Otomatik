(ns rocks.empty.otomatik.irc-client
    (:require [rocks.empty.otomatik.connection-builder :as connection-builder])
    (:require [rocks.empty.otomatik.irc-commands :as irc-commands])
    (:require [rocks.empty.otomatik.irc-handlers :as irc-handlers])
    (:require [rocks.empty.otomatik.plugin-helper :as plugin-helper])
    (:require [rocks.empty.otomatik.default-plugins.channel-watcher :as channel-watcher])
    (:require [clojure.tools.logging :as log])
    (:require [clojure.core.async :as a :refer [>! <!! go go-loop chan]])
  )

(defn main-loop-consumer
  [connection plugins state]
  (loop []
    (when-let [packet (<!! (:in connection))]
      (log/trace "Recieved from :in channel:" packet)
      (irc-handlers/handle packet connection) ; Core handlers
      (plugin-helper/message-recieved connection plugins state packet)
      (recur))))

(defn start-in-provider
  [connection]
  (go-loop []
    (when (.ready (:reader connection))
      (let [line (.readLine (:reader connection))]
        (log/trace "Adding the following line to :in," line)
        (>! (:in connection) {
          :raw line
          :nickname @(:nickname connection)
          :message (irc-commands/parse-message line)
        })))
  (recur)))

(defn start-out-consumer
  [connection]
  (go-loop []
    (when-let [line (<!! (:out connection))]
      (log/trace "Writing to socket" line)
      (.write (:writer connection) (str line "\r\n"))
      (.flush (:writer connection))
      (recur))))

(defn init-connection
  [connection options]
    (log/debug "Registering NICK and USER")
    (irc-commands/irc-command (:writer connection) "NICK" (:nickname options))
    (irc-commands/irc-command (:writer connection) "USER" (:realname options)  "8" "*" ":" "Otomatik"))

(defn main-loop
  [options]
  (let [input-channel (chan) output-channel (chan)]
    (let [
      channels (ref {})
      connection (connection-builder/create options input-channel output-channel)
      plugins (concat [(channel-watcher/registration channels)] (:plugins options))
      state {:channels channels}
      ]
      (log/tracef "Plugins: %s" (vec plugins))
      (init-connection connection options)
      (plugin-helper/init-plugins connection plugins state)
      (log/trace "Starting up threads.")
      (start-in-provider connection)
      (start-out-consumer connection)
      (log/trace "Threads started. Starting main loop.")
      (main-loop-consumer connection plugins state))))
