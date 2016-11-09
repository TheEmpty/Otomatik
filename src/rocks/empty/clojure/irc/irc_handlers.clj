(ns rocks.empty.clojure.irc.irc-handlers
	(:require [rocks.empty.clojure.irc.irc-commands :as irc-commands]))

(defmulti handle "Handles IRC commands based on the given IRC command" (fn [packet] (:command (:message packet))))

(defmethod handle "PING" [packet]
  (irc-commands/irc-command (:writer (:connection packet)) "PONG"))

(defmethod handle "433" [packet]
  (.println System/err "Nickname already in use.")
  (System/exit -1))

; Helper for (idea for) example plugin.
(defn respondTimeZone [connection, chan, zone]
  (irc-commands/irc-command (:writer connection) "PRIVMSG" chan
               (str ":" zone " => "
                    (.toString (java.time.LocalDateTime/now (java.time.ZoneId/of zone))))))

(defmethod handle "PRIVMSG" [packet]
  (def chan (nth (:params (:message packet)) 0))
  (def msg (nth (:params (:message packet)) 1))

  ; Idea for example plugin
  (when (= msg "!time")
    (respondTimeZone (:connection packet) chan "America/Los_Angeles")
    (respondTimeZone (:connection packet) chan "Europe/London")
    (respondTimeZone (:connection packet) chan "Japan"))

  (println (str "Recieved on " chan " from " (:nickname (:prefix (:message packet))) ": " msg)))

(defmethod handle :default [packet]
  (println (str "Recieved an unknown command, " (:command (:message packet)))))