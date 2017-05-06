(ns rocks.empty.otomatik.plugin-helper
    (:require [clojure.tools.logging :as log])
    (:require [rocks.empty.otomatik.channel-wrapper :as channel-wrapper])
    (:require [clojure.core.async :as a :refer [>! go]]))

(defmacro idgaf
  [& questionable-code]
  `(try
    (do ~@questionable-code)
    (catch Exception e# (log/warn e#))))

(defn build-state-map
  [connection state]
  (merge
    state
    {:nickname (:nickname connection)}))

(defn write-plugin-result
  "Used for non-versioned plugins"
  [output-channel result]
  (when-not (= nil result)
    (if (string? result)
      (go (>! output-channel (str result)))
      (doseq [line result] (go (>! output-channel (str line)))))))

(defn build-message
  [packet]
  (conj (:message packet) {:raw (:raw packet)}))

(defn init-plugin-zero-dot-two
  [connection state plugin]
  (when (:on-connect plugin)
    (log/debug "Calling" (get plugin :name "a plugin's") ":on-connect by" (get plugin :author "an uknown author."))
    (idgaf
      ((:on-connect plugin)
        (channel-wrapper/create (:out connection))
        (build-state-map connection state)))))

(defn init-plugin-original
  [connection state plugin]
  (when (:init plugin)
    (log/debug "Calling" (get plugin :name "a plugin's") ":init by" (get plugin :author "an uknown author."))
    (idgaf (write-plugin-result (:out connection)
      ((:init plugin) (build-state-map connection state))))))

(defn init-plugins
  "Starts plugins that define a starting method."
  [connection plugins state]
  (doseq [plugin plugins]
    (if (= 0.2 (get plugin :otomatik-version 0))
      (init-plugin-zero-dot-two connection state plugin)
      (init-plugin-original connection state plugin))))

(defn message-recieved-zero-dot-two
  [connection plugin state packet]
  (if (:on-message-recieved plugin)
    (let [
      write-fn (channel-wrapper/create (:out connection))
      message (build-message packet)
      state-map (build-state-map connection state)
      ]
        (idgaf ((:on-message-recieved plugin) write-fn message state-map)))))

(defn message-recieved-original
  [connection plugin packet]
  (if (:function plugin)
    (idgaf (write-plugin-result
      (:out connection)
      ((:function plugin) packet)))))

(defn message-recieved
  [connection plugins state packet]
  (doseq [plugin plugins]
    (if (= 0.2 (get plugin :otomatik-version 0))
      (message-recieved-zero-dot-two connection plugin state packet)
      (message-recieved-original connection plugin packet))))
