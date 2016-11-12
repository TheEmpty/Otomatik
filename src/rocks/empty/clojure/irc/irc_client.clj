(ns rocks.empty.clojure.irc.irc-client
    (:import java.net.Socket)
    (:import java.io.BufferedWriter)
    (:import java.io.BufferedReader)
    (:import java.io.InputStreamReader)
    (:import java.io.OutputStreamWriter)
    (:require [rocks.empty.clojure.irc.irc-commands :as irc-commands])
    (:require [rocks.empty.clojure.irc.irc-handlers :as irc-handlers])
  )

(defn irc-read-motd
  "Moves the reader past the Message of the Day"
  [reader]
  (while
    (= -1 (.indexOf (locking reader (.readLine reader)) "376"))))

(defn connect
  "Returns a connection map to the IRC server"
  [options, pool]

  (let
    [
      server (:server options)
      port (:port options)
      socket (new Socket (str server) (long port))
      outputStream (new OutputStreamWriter (.getOutputStream socket))
      outputBuffer (new BufferedWriter outputStream)
      inputStream (new InputStreamReader (.getInputStream socket))
      inputBuffer (new BufferedReader inputStream)
    ]

    (irc-commands/irc-command outputBuffer "NICK" (:nickname options))
    (irc-commands/irc-command outputBuffer "USER" (:realname options)  "8" "*" ":" "EmptyDotRocks")
    (irc-read-motd inputBuffer)

    ; Calls init on plugins that define it
    (let [connection {:reader inputBuffer :writer outputBuffer :socket socket}]
      (doseq [plugin (:plugins options)]
        (if (:init plugin)
          (.submit pool (fn [] ((:init plugin) connection)))))
      connection)))

(defn main-loop
  [connection, plugins, pool]
  (while (not (.isClosed (:socket connection)))
    (let [
      inputBuffer (:reader connection)
      line (locking inputBuffer (.readLine inputBuffer))
      message (irc-commands/parse-message line)
      packet {:message message :raw line :connection connection}
      ]
      (irc-handlers/handle packet)
      (doseq [plugin plugins]
      (if (:function plugin)
      (.submit pool (fn [] ((:function plugin) packet))))))))

(defn bot
  [options]
  (let [
    pool (java.util.concurrent.Executors/newFixedThreadPool 16)
    connection (connect options pool)
    ]
    (main-loop connection (:plugins options) pool)
    (.shutdown pool)
    ))
