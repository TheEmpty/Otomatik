(ns rocks.empty.otomatik.irc-client
    (:import java.net.Socket)
    (:import javax.net.ssl.SSLSocketFactory)
    (:import java.io.BufferedWriter)
    (:import java.io.BufferedReader)
    (:import java.io.InputStreamReader)
    (:import java.io.OutputStreamWriter)
    (:require [rocks.empty.otomatik.irc-commands :as irc-commands])
    (:require [rocks.empty.otomatik.irc-handlers :as irc-handlers])
    (:require [clojure.core.async
              :as a
              :refer [>! <! >!! <!! go go-loop chan buffer close! thread alts! alts!! timeout]])
  )

(defn debug "Quick/temporary until logging" [& args] (println args))

(defn create-socket
  "Creates the socket used to communicate with the server. Given server, port, and optional ssl flag."
  [options]
  (let
    [
     port (if (string? (:port options)) (Long/parseLong (:port options)) (long (:port options)))
     server (str (:server options))
     ssl (get options :ssl)
     ]

    (if (= ssl true)
      (.createSocket (SSLSocketFactory/getDefault) server port)
      (new Socket server port))))

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
        (go ((:init plugin) {:out output-channel :nickname nickname}))))

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
      (debug (str "Recieved from channel: " packet))
      (irc-handlers/handle packet connection)
      (doseq [plugin plugins]
        (if (:function plugin)
          (go ((:function plugin) packet))))
      (recur))))

(defn main-loop-provider
  [connection]
  (go-loop []
    (when-let [line (.readLine (:reader connection))]
      (>! (:in connection) {
        :raw line
        :out (:out connection)
        :nickname (:nickname connection)
        :message (irc-commands/parse-message line)
      })
      (recur))))

(defn main-loop-writer
  [connection]
  (go-loop []
    (when-let [line (<!! (:out connection))]
      (debug "Writing " line)
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

