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

    (if (= ssl true)
      (.createSocket (SSLSocketFactory/getDefault) server port)
      (new Socket server port))))

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

    (irc-commands/irc-command outputBuffer "NICK" (:nickname options))
    (irc-commands/irc-command outputBuffer "USER" (:realname options)  "8" "*" ":" "EmptyDotRocks")

    ; Calls init on plugins that define it.
    (doseq [plugin (:plugins options)]
      (if (:init plugin)
        (go (write-plugin-result output-channel ((:init plugin) {:nickname nickname})))))

    {
     :in input-channel
     :out output-channel
     :reader inputBuffer
     :writer outputBuffer
     :socket socket
     :nickname nickname
    }))

(defn main-loop-consumer
  [connection plugins]
  (loop []
    (when-let [packet (<!! (:in connection))]
      (log/trace "Recieved from channel: " packet)
      (irc-handlers/handle packet connection)
      (doseq [plugin plugins]
        (if (:function plugin)
          (go (write-plugin-result
            (:out connection)
            ((:function plugin) packet)))))
      (recur))))

(defn main-loop-provider
  [connection]
  (go-loop []
    (when-let [line (.readLine (:reader connection))]
      (>! (:in connection) {
        :raw line
        :nickname @(:nickname connection)
        :message (irc-commands/parse-message line)
      })
      (recur))))

(defn main-loop-writer
  [connection]
  (go-loop []
    (when-let [line (<!! (:out connection))]
      (log/trace "Writing " line)
      (.write (:writer connection) (str line "\r\n"))
      (.flush (:writer connection))
      (recur))))

(defn main-loop
  [options]
  (let [input-channel (chan) output-channel (chan)]
    (let [connection (connect options input-channel output-channel)]
      (main-loop-provider connection)
      (main-loop-writer connection)
      (main-loop-consumer connection (:plugins options)))))

