(ns rocks.empty.clojure.irc.irc-client
    (:import java.net.Socket)
    (:import java.io.BufferedWriter)
    (:import java.io.BufferedReader)
    (:import java.io.InputStreamReader)
    (:import java.io.OutputStreamWriter)
	(:require [rocks.empty.clojure.irc.irc-commands :as irc-commands])
	(:require [rocks.empty.clojure.irc.irc-handlers :as irc-handlers])
  )

; TODO: once we get plugins, the auto-join can become a plugin. Thus removing this.
(defn irc-read-motd
  "Moves the reader past the Message of the Day"
  [reader]
  (while
    (and
      (def line (.readLine reader))
      (= -1 (.indexOf line "376")))))

(defn connect
  "Returns a connection map to the IRC server"
  [options]

  (def server (:server options))
  (def port (:port options))
  (def socket (new Socket (str server) (long port)))
  (def outputStream (new OutputStreamWriter (.getOutputStream socket)))
  (def outputBuffer (new BufferedWriter outputStream))
  (def inputStream (new InputStreamReader (.getInputStream socket)))
  (def inputBuffer (new BufferedReader inputStream))

  (irc-commands/irc-command outputBuffer "NICK" (:nickname options))
  (irc-commands/irc-command outputBuffer "USER" (:realname options)  "8" "*" ":" "EmptyDotRocks")
  (irc-read-motd inputBuffer)
  (irc-commands/irc-command outputBuffer "JOIN" (:channel options))

  {:reader inputStream :writer outputStream :socket socket})

(defn main-loop
  [connection]
  (while (not (.isClosed (:socket connection)))
    (def line (.readLine inputBuffer))
    (def message (irc-commands/parse-message line))
    (irc-handlers/handle {
             :message message
             :raw line
             :connection connection
             })))

(defn bot
  [options]
  (main-loop (connect options)))

