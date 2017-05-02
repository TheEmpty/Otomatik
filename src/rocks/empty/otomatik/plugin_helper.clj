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

(defn init-plugins
  "Starts plugins that define a starting method."
  [connection plugins state]
  (doseq [plugin plugins]
    (if (= 0.2 (get plugin :otomatik_version 0))
      (when (:on-connect plugin)
        (log/debug "Calling" (get plugin :name "a plugin's") ":on-connect by" (get plugin :author "an uknown author."))
        (idgaf
          ((:on-connect plugin) (channel-wrapper/create (:out connection)) (build-state-map connection state))))
      (when (:init plugin)
        (log/debug "Calling" (get plugin :name "a plugin's") ":init by" (get plugin :author "an uknown author."))
        (idgaf (write-plugin-result (:out connection)
            ((:init plugin) (build-state-map connection))))))))

(defn message-recieved
  [connection plugins state packet]
  (doseq [plugin plugins]
    (if (= 0.2 (get plugin :otomatik_version 0))
      (if (:on-message-recieved plugin)
        (idgaf ((:on-message-recieved plugin) (channel-wrapper/create (:out connection)) (build-message packet) (build-state-map connection state))))
      (if (:function plugin)
        (idgaf (write-plugin-result
          (:out connection)
          ((:function plugin) packet)))))))
