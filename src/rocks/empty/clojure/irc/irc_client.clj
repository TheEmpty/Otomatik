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
  "Returns a connection object to the IRC server"
  [server, port, nickname, realname, channel]
  
  (def socket (new Socket server port))
  (def outputStream (new OutputStreamWriter (.getOutputStream socket)))
  (def outputBuffer (new BufferedWriter outputStream))
  (def inputStream (new InputStreamReader (.getInputStream socket)))
  (def inputBuffer (new BufferedReader inputStream))

  (irc-commands/irc-command outputBuffer "NICK" nickname)
  (irc-commands/irc-command outputBuffer "USER" realname "8" "*" ":" "Empty Bot")
  (irc-read-motd inputBuffer)
  (irc-commands/irc-command outputBuffer "JOIN" channel)

  {:reader inputStream :writer outputStream :socket socket})

(defn main-loop
  [connection]
  (while (not (.isClosed (:socket connection)))
    (def line (.readLine inputBuffer))
    (println (str ">>> " line))
    (def message (irc-commands/parse-message line))
    (println (str "  > " message))
    (irc-handlers/handle {
             :message message
             :raw line
             :connection connection
             })))
