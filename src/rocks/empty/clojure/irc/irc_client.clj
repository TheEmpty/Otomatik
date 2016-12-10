(ns rocks.empty.clojure.irc.irc-client
    (:import java.net.Socket)
    (:import javax.net.ssl.SSLSocketFactory)
    (:import java.io.BufferedWriter)
    (:import java.io.BufferedReader)
    (:import java.io.InputStreamReader)
    (:import java.io.OutputStreamWriter)
    (:require [rocks.empty.clojure.irc.irc-commands :as irc-commands])
    (:require [rocks.empty.clojure.irc.irc-handlers :as irc-handlers])
  )

(defn create-socket
  "Creates the socket used to communicate with the server. Given server, port, and optional ssl flag."
  [options]
  (let
    [
     port (if (string? (:port options)) (java.lang.Long/parseLong (:port options)) (long (:port options)))
     server (str (:server options))
     ssl (get options :ssl)
     ]

    (if (= ssl true)
      (.createSocket (SSLSocketFactory/getDefault) server port)
      (new Socket server port))))

(defn connect
  "Returns a connection map to the IRC server"
  [options, pool]

  (let
    [
      socket (create-socket options)
      outputBuffer (new BufferedWriter (new OutputStreamWriter (.getOutputStream socket)))
      inputBuffer (new BufferedReader (new InputStreamReader (.getInputStream socket)))
      nickname (ref (:nickname options))
    ]

    (irc-commands/irc-command outputBuffer "NICK" (:nickname options))
    (irc-commands/irc-command outputBuffer "USER" (:realname options)  "8" "*" ":" "EmptyDotRocks")

    ; Calls init on plugins that define it - TODO: timeouts.
    (let [connection {:reader inputBuffer :writer outputBuffer :socket socket :nickname nickname}]
      (doseq [plugin (:plugins options)]
        (if (:init plugin)
          (.submit pool (fn [] ((:init plugin) connection)))))
      connection)))

(defn main-loop
  [connection, plugins, pool]
  ; TODO: This loop doesn't exit when the connection is closed
  ; if it's still waiting for a line
  (while (not (.isClosed (:socket connection)))
    (let [
      inputBuffer (:reader connection)
      line (locking inputBuffer (.readLine inputBuffer))
      message (irc-commands/parse-message line)
      packet {:message message :raw line :connection connection}
      ]
      (irc-handlers/handle packet)
      (doseq [plugin plugins]
        (if (:function plugin) ; TODO: timeouts.
          (.submit pool (fn [] ((:function plugin) packet))))))))

(defn bot
  [options]
  (let [
    pool (java.util.concurrent.Executors/newFixedThreadPool 16) ; TODO: thread pool should be cofigurable
    connection (connect options pool)
    ]
    (main-loop connection (:plugins options) pool)
    (.shutdown pool)))
