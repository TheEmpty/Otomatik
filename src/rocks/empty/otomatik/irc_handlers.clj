; These are internal "plugins" to support required IRC commands and responses.

(ns rocks.empty.otomatik.irc-handlers
  (:require [rocks.empty.otomatik.irc-commands :as irc-commands])
  (:require [clojure.core.async
               :as a
               :refer [>! <! >!! <!! go chan buffer close! thread alts! alts!! timeout]]))

(defn close-connection [packet]
  ; NOTE: this should be warn level
  (.println System/err (str "Closing connection due to " (:raw packet)))
  (let [connection (:connection packet)]
    ; Close dem channels too
    ; NOTE: this currently doesn't work the best, can cause JVM to quit.
    (.shutdownInput (:socket connection))
    (.shutdownOutput (:socket connection))
    (.close (:socket connection))
    (.close (:reader connection))
    (.close (:writer connection))))

(defmulti handle
  "Handles IRC commands based on the given IRC command"
  (fn [packet] (:command (:message packet))))

(defmethod handle "PING" [packet]
  "This is sent by the IRC server to verify the client is still up and running."
  (let
    [
      params (:params (:message packet))
      last-param (nth params (- 1 (count params)))
    ]
    (>!! (:out packet) (str "PONG " last-param))))

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
    (if (= prev-nick @(:nickname (:connection packet)))
      (dosync (ref-set (:nickname (:connection packet)) new-nick)))))

; NO-OP for unknown commands
(defmethod handle :default [packet] nil)
