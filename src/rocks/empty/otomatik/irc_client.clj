(ns rocks.empty.otomatik.irc-client
    (:import java.net.Socket)
    (:import javax.net.ssl.SSLSocketFactory)
    (:import java.io.BufferedWriter)
    (:import java.io.BufferedReader)
    (:import java.io.InputStreamReader)
    (:import java.io.OutputStreamWriter)
    (:require [rocks.empty.otomatik.irc-commands :as irc-commands])
    (:require [rocks.empty.otomatik.irc-handlers :as irc-handlers])
    (:require [clojure.tools.logging :as log])
    (:require [clojure.core.async
              :as a
              :refer [>! <! >!! <!! go go-loop chan buffer close! thread alts! alts!! timeout]])
  )

(defn create-socket
  "Creates the socket used to communicate with the server. Given server, port, and optional ssl flag."
  [options]
  (let
    [
     port (if (string? (:port options)) (Long/parseLong (:port options)) (long (:port options)))
     server (str (:server options))
     ssl (get options :ssl)
     ]
    (log/debugf "Trying to connect to %s:%s with ssl = %s" server port (= true ssl))

    ; there is probably a better way to do this...
    (let
      [
        socket (if (= ssl true) (.createSocket (SSLSocketFactory/getDefault) server port) (new Socket server port))
      ]
      (log/debugf "Connected with %s" socket)
      socket)))

(defn write-plugin-result
  [output-channel result]
  (when-not (= nil result)
    (if (string? result)
      (go (>! output-channel (str result)))
      (doseq [line result] (go (>! output-channel (str line)))))))

(defn connect
  "Returns a connection map to the IRC server"
  [options input-channel output-channel]

  (let
    [
      socket (create-socket options)
      outputBuffer (new BufferedWriter (new OutputStreamWriter (.getOutputStream socket)))
      inputBuffer (new BufferedReader (new InputStreamReader (.getInputStream socket)))
      nickname (ref (:nickname options))
    ]

    (log/debug "Registering NICK and USER")
    (irc-commands/irc-command outputBuffer "NICK" (:nickname options))
    (irc-commands/irc-command outputBuffer "USER" (:realname options)  "8" "*" ":" "EmptyDotRocks")

    {
     :in input-channel
     :out output-channel
     :reader inputBuffer
     :writer outputBuffer
     :socket socket
     :nickname nickname
    }))

(defn init-plugins
  "Calls init on plugins that define it."
  [connection plugins]
  (doseq [plugin plugins]
    (when (:init plugin)
      (log/debug "Calling a plugin's :init by" (get plugin :author))
      (go (write-plugin-result (:out connection)
        ((:init plugin) {:nickname (:nickname connection)}))))))

(defn main-loop-consumer
  [connection plugins]
  (loop []
    (when-let [packet (<!! (:in connection))]
      (log/trace "Recieved from channel:" packet)
      (irc-handlers/handle packet connection)
      (doseq [plugin plugins]
        (if (:function plugin)
          (go (write-plugin-result
            (:out connection)
            ((:function plugin) packet)))))
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
      (log/trace "Writing" line)
      (.write (:writer connection) (str line "\r\n"))
      (.flush (:writer connection))
      (recur))))

(defn main-loop
  [options]
  (let [input-channel (chan) output-channel (chan)]
    (let [connection (connect options input-channel output-channel)]
      (init-plugins connection (:plugins options))
      (log/trace "Starting up threads.")
      (start-in-provider connection)
      (start-out-consumer connection)
      (log/trace "Threads started. Starting main loop.")
      (main-loop-consumer connection (:plugins options)))))

