(ns rocks.empty.otomatik.irc-client
    (:require [rocks.empty.otomatik.connection-builder :as connection-builder])
    (:require [rocks.empty.otomatik.irc-commands :as irc-commands])
    (:require [rocks.empty.otomatik.irc-handlers :as irc-handlers])
    (:require [rocks.empty.otomatik.channel-wrapper :as channel-wrapper])
    (:require [clojure.tools.logging :as log])
    (:require [clojure.core.async
              :as a
              :refer [>! <! >!! <!! go go-loop chan buffer close! thread alts! alts!! timeout]])
  )

; TODO: also pull out plugins

(defn write-plugin-result
  [output-channel result]
  (when-not (= nil result)
    (if (string? result)
      (go (>! output-channel (str result)))
      (doseq [line result] (go (>! output-channel (str line)))))))

(defn init-plugins
  "Starts plugins that define a starting method."
  [connection plugins]
  (doseq [plugin plugins]
    (if (>= 0.2 (get plugin :otomatik_version 0))
      (when (:on-connect plugin)
        (log/debug "Calling" (get plugin :name "a plugin's") ":on-connect by" (get plugin :author "an uknown author."))
        ((:on-connect plugin) (channel-wrapper/create (:out connection))))
      (when (:init plugin)
        (log/debug "Calling" (get plugin :name "a plugin's") ":init by" (get plugin :author "an uknown author."))
        (if (>= 0.2 (get plugin :otomatik_version 0))
          ((:on-connect plugin) (channel-wrapper/create (:out connection)))
          (go (write-plugin-result (:out connection)
            ((:init plugin) {:nickname (:nickname connection)}))))))))

(defn main-loop-consumer
  [connection plugins]
  (loop []
    (when-let [packet (<!! (:in connection))]
      (log/trace "Recieved from :in channel:" packet)
      (irc-handlers/handle packet connection)
      (doseq [plugin plugins]
        (if (>= 0.2 (get plugin :otomatik_version 0))
          (if (:on-message-recieved plugin)
            ((:on-message-recieved plugin) (channel-wrapper/create (:out connection)) packet) 
          (if (:function plugin)
            (go (write-plugin-result
              (:out connection)
              ((:function plugin) packet)))))))
      (recur))))

(defn start-in-provider
  [connection]
  (go-loop []
    (when-let [line (.readLine (:reader connection))]
      (log/trace "Adding the following line to :in," line)
      (>! (:in connection) {
        :raw line
        :nickname @(:nickname connection)
        :message (irc-commands/parse-message line)
      })
      (recur))))

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
    (irc-commands/irc-command (:writer connection) "USER" (:realname options)  "8" "*" ":" "EmptyDotRocks"))

(defn main-loop
  [options]
  (let [input-channel (chan) output-channel (chan)]
    (let [connection (connection-builder/create options input-channel output-channel)]
      (init-connection connection options)
      (init-plugins connection (:plugins options))
      (log/trace "Starting up threads.")
      (start-in-provider connection)
      (start-out-consumer connection)
      (log/trace "Threads started. Starting main loop.")
      (main-loop-consumer connection (:plugins options)))))

