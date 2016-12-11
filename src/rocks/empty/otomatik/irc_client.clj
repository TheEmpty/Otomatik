(ns rocks.empty.otomatik.irc-client
    (:import java.net.Socket)
    (:import javax.net.ssl.SSLSocketFactory)
    (:import java.io.BufferedWriter)
    (:import java.io.BufferedReader)
    (:import java.io.InputStreamReader)
    (:import java.io.OutputStreamWriter)
    (:require [rocks.empty.otomatik.irc-commands :as irc-commands])
    (:require [rocks.empty.otomatik.irc-handlers :as irc-handlers])
    (:require [clojure.core.async
              :as a
              :refer [>! <! >!! <!! go chan buffer close! thread alts! alts!! timeout]])
  )

(defn debug "Quick/temporary until logging" [& args] (println args))

; TODO: this won't be needed anymore
(defn run-with-timeout
  "Runs a function, for no longer than the given time in ms."
  [timeout-ms function]
  (let [future-body (future (function))]
    (let [future-value (deref future-body timeout-ms :timeout)]
      (debug (str function " " (if realized? (str "realized with " future-value) "failed to realize.")))
      (if (realized? future-body) future-value (do (future-cancel future-body) nil)))))

; TODO: this won't be needed anymore
(defn submit-plugin
  "Helper function, submits plugin's function with timeout to the given pool."
  [pool plugin action argument]
  (.submit pool (fn [] (run-with-timeout
                  (get plugin (keyword (str action "-timeout")) 2000)
                  (fn [] (((keyword action) plugin) argument))))))

(defn create-socket
  "Creates the socket used to communicate with the server. Given server, port, and optional ssl flag."
  [options]
  (let
    [
     port (if (string? (:port options)) (Long/parseLong (:port options)) (long (:port options)))
     server (str (:server options))
     ssl (get options :ssl)
     ]

    (if (= ssl true)
      (.createSocket (SSLSocketFactory/getDefault) server port)
      (new Socket server port))))

(defn connect
  "Returns a connection map to the IRC server"
  [options]

  (let
    [
      socket (create-socket options)
      outputBuffer (new BufferedWriter (new OutputStreamWriter (.getOutputStream socket)))
      inputBuffer (new BufferedReader (new InputStreamReader (.getInputStream socket)))
      nickname (ref (:nickname options))
    ]

    (irc-commands/irc-command outputBuffer "NICK" (:nickname options))
    (irc-commands/irc-command outputBuffer "USER" (:realname options)  "8" "*" ":" "EmptyDotRocks")
    {:reader inputBuffer :writer outputBuffer :socket socket :nickname nickname}))

    ; Calls init on plugins that define it.
    ; TODO: convert to channels
    ;(let [connection {:reader inputBuffer :writer outputBuffer :socket socket :nickname nickname}]
     ; (doseq [plugin (:plugins options)] (if (:init plugin) (submit-plugin pool plugin "init" connection)))
      ;connection)))

; TODO: move to writer streams too
(defn main-loop-consumer
  [channel plugins]
  (while true
    (let [packet (<!! channel)]
      (debug (str "Recieved from channel: " packet))
      (irc-handlers/handle packet)
      (doseq [plugin plugins]
        (if (:function plugin)
          (go ((:function plugin) packet)))))))
      ; TODO: call plugins (then they'll write to the stream)

(defn main-loop-provider
  [connection channel]
  (go (while true
    (let [line (.readLine (:reader connection))]
      (>! channel {
        :raw line
        :connection connection
        :message (irc-commands/parse-message line)
      })))))

(defn main-loop
  [connection plugins]
  (let [input-channel (chan)]
    (main-loop-provider connection input-channel)
    (main-loop-consumer input-channel plugins)))

; TODO: this should probably be in it's own file so it's not exposing all the extra stuff.
(defn bot
  [options]
  (let [
    connection (connect options)
    ]
    (main-loop connection (:plugins options))))
