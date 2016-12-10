; These are internal "plugins" to support required IRC commands and responses.

(ns rocks.empty.otomatik.irc-handlers
	(:require [rocks.empty.otomatik.irc-commands :as irc-commands]))

(defn close-connection [packet]
  (.println System/err (str "Closing connection due to " (:raw packet)))
  (let [connection (:connection packet)]
    (locking (:reader connection) (.close (:reader connection)))
    (locking (:writer connection) (.close (:writer connection)))
    (locking (:socket connection) (.close (:socket connection)))))

(defmulti handle
  "Handles IRC commands based on the given IRC command"
  (fn [packet] (:command (:message packet))))

(defmethod handle "PING" [packet]
  "This is sent by the IRC server to verify the client is still up and running."
  (let
    [
      params (:params packet)
      last-param (nth params (- 1 (count params)))
    ]
    (irc-commands/irc-command (:writer (:connection packet)) "PONG" last-param)))

(defmethod handle "ERROR" [packet]
  "This is sent by the server indicating a fatal issue."
  (close-connection packet))

(defmethod handle "KILL" [packet]
  "This is sent by the server before it closes the connection."
  (close-connection packet))

(defmethod handle "NICK" [packet]
  "This keeps track of the bot's nickname."
  (let [
    prev-nick (first (:prefix (:message packet)))
    new-nick (last (:params (:message packet)))
    ]
    (if (= prev-nick (deref (:nickname (:connection packet))))
      (dosync (ref-set (:nickname (:connection packet)) new-nick)))))

; NO-OP for unknown commands
(defmethod handle :default [packet] nil)
