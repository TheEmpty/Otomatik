; Written by Mohammad El-Abid
; First clojure program, so forgive me if it's messy or anti-pattern.
; Rules: https://tools.ietf.org/html/rfc2812#section-2.3.1

(ns rocks.empty.clojure.irc.bot-core
    (:gen-class)
    (:import java.net.Socket)
    (:import java.io.BufferedWriter)
    (:import java.io.BufferedReader)
    (:import java.io.InputStreamReader)
    (:import java.io.OutputStreamWriter)
  )

; TODO: config/arguments
(def server "irc.freenode.net")
(def port 6667)
(def nickname "Empty-testbot")
(def login "Empty-testbot")
(def channel "#emptytest")

; Regex used - TODO: bring the other one up here, also privmessage doesn't need to exist anymore
(def irc-username-regex #"^:(.+?)!~(.+?)@(.+?)$")
(def irc-privmessage-regex #"^(.+?) :(.+?)$")

(defn irc-command
  [writer, & args]
  (.write writer (str (clojure.string/join " " args) "\r\n"))
  (.flush writer))

; TODO: once we get plugins, the auto-join can become a plugin. Thus removing this.
(defn irc-read-motd
  "Moves the reader past the Message of the Day"
  [reader]
  (while
    (and
      (def line (.readLine reader))
      (= -1 (.indexOf line "376")))))

; TODO: this isn't correct to standards
(defn parse-user
  "Returns the nickname, realname, and host of a given IRC user string"
  [line]
  (def matcher (re-matcher irc-username-regex line))
  (def results (re-find matcher))
  {:nickname (nth results 1) :realname (nth results 2) :host (nth results 3)})

; TODO: pass in server and port.
(defn irc-connect
  "Returns a connection object to the IRC server"
  [nickname, realname, channel]
  
  (def socket (new Socket server port))
  (def outputStream (new OutputStreamWriter (.getOutputStream socket)))
  (def outputBuffer (new BufferedWriter outputStream))
  (def inputStream (new InputStreamReader (.getInputStream socket)))
  (def inputBuffer (new BufferedReader inputStream))

  (irc-command outputBuffer "NICK" nickname)
  (irc-command outputBuffer "USER" realname "8" "*" ":" "Empty Bot")
  (irc-read-motd inputBuffer)
  (irc-command outputBuffer "JOIN" channel)

  {:reader inputStream :writer outputStream}
  )

(defn echo [connection, channel, user, message]
  (irc-command (:writer connection) "PRIVMSG" channel (str ":" (:nickname user) " did you seriously just say " message)))

(defmulti handle "Handles IRC commands based on the given IRC command" (fn [packet] (:command (:message packet))))

(defmethod handle "PING" [packet]
  (irc-command (:writer (:connection packet)) "PONG"))

(defmethod handle "433" [packet]
  (.println System/err "Nickname already in use.")
  (System/exit -1))

(defmethod handle "PRIVMSG" [packet]
  (def chan (nth (:params (:message packet)) 0))
  (def msg (nth (:params (:message packet)) 1))
  ; (echo (:connection packet) chan (:prefix (:message packet)) msg)

  (println (str "Recieved on " chan " from " (:nickname (:prefix (:message packet))) ": " msg)))

(defmethod handle :default [packet]
  (println (str "Recieved an unknown command, " (:command (:message packet)))))

(defn parse-params [params]
  "Parses the parameter part of an IRC message"
  (if (= params nil) nil
    (do
      (def trimmed (clojure.string/trim params))
      (def trailing-split (clojure.string/split trimmed #":" 2))
      (def bare (clojure.string/split (nth trailing-split 0) #" "))

      (if (= 2 (count trailing-split))
        (concat bare (list (nth trailing-split 1)))
        bare))))

; TODO: this isn't accurate - needs to handle name[[!user]@host]
(defn parse-prefix [prefix]
  "Parses the prefix part of an IRC message"
  (if (= prefix nil) nil
    (do
      (def trimmed (clojure.string/trim prefix))
      (parse-user trimmed))))

(defn parse-message [line]
  (def parsed (re-find (re-matcher #"^(:.+? )?(.+?)( .+?)?$" line)))
  (def prefix (parse-prefix (nth parsed 1)))
  (def command (nth parsed 2))
  (def params (parse-params (nth parsed 3)))

  {:prefix prefix :command command :params params})

(defn irc-loop
  [connection]
  (while true
    (def line (.readLine inputBuffer))
    (println (str ">>> " line))
    (def message (parse-message line))
    (println (str "  > " message))
    (handle {
             :message message
             :raw line
             :connection connection
             })))

(defn -main
  [& args]
  (def connection (irc-connect nickname login channel))
  (irc-loop connection))
