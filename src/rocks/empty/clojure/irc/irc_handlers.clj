; These are internal "plugins" to support required IRC commands and responses.

(ns rocks.empty.clojure.irc.irc-handlers
	(:require [rocks.empty.clojure.irc.irc-commands :as irc-commands]))

(defn close-connection [packet]
  (.println System/err (str "Closing connection due to " (apply str (:params (:message packet)))))
  (.close (:reader (:connection packet)))
  (.close (:writer (:connection packet)))
  (.close (:socket (:connection packet))))

(defmulti handle "Handles IRC commands based on the given IRC command" (fn [packet] (:command (:message packet))))

(defmethod handle "PING" [packet]
  "This is sent by the IRC server to verify the client is still up and running."
  (def params (:params packet))
  (def last-param (nth params (- 1 (count params))))
  (irc-commands/irc-command (:writer (:connection packet)) "PONG " last-param))

(defmethod handle "ERROR" [packet]
  "This is sent by the server indicating a fatal issue."
  (close-connection packet))

(defmethod handle "KILL" [packet]
  "This is sent by the server before it closes the connection."
  (close-connection packet))

; NO-OP for unknown commands
(defmethod handle :default [packet] nil)
