(ns rocks.empty.otomatik.irc-handlers
  (:require [rocks.empty.otomatik.irc-commands :as irc-commands])
  (:require [clojure.tools.logging :as log])
  (:require [clojure.core.async :as a :refer [>!! close!]]))

(defn close-connection [packet connection]
  (log/fatal "Closing connection due to " (:raw packet))
  (close! (:out connection))
  (close! (:in connection))
  (.close (:socket connection))
  (.close (:reader connection))
  (.close (:writer connection)))

(defmulti handle
  "Handles IRC commands based on the given IRC command"
  (fn [packet connection] (:command (:message packet))))

(defmethod handle "PING" [packet connection]
  "This is sent by the IRC server to verify the client is still up and running."
  (let
    [
      params (:params (:message packet))
      last-param (nth params (- 1 (count params)))
    ]
    (>!! (:out connection) (str "PONG " last-param))))

(defmethod handle "ERROR" [packet connection]
  "This is sent by the server indicating a fatal issue."
  (close-connection packet connection))

(defmethod handle "KILL" [packet connection]
  "This is sent by the server before it closes the connection."
  (close-connection packet connection))

(defmethod handle "NICK" [packet connection]
  "This keeps track of the bot's nickname."
  (let [
    prev-nick (first (:prefix (:message packet)))
    new-nick (last (:params (:message packet)))
    ]
    (if (= prev-nick @(:nickname connection))
      (dosync (ref-set (:nickname connection) new-nick)))))

; NO-OP for unknown commands
(defmethod handle :default [packet connection] nil)
