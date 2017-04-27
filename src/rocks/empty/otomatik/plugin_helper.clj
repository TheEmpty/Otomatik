(ns rocks.empty.otomatik.plugin-helper
    (:require [clojure.tools.logging :as log])
    (:require [rocks.empty.otomatik.channel-wrapper :as channel-wrapper])
    (:require [clojure.core.async :as a :refer [>! go]]))

(defn write-plugin-result
  "Used for non-versioned plugins"
  [output-channel result]
  (when-not (= nil result)
    (if (string? result)
      (go (>! output-channel (str result)))
      (doseq [line result] (go (>! output-channel (str line)))))))

(defn init-plugins
  "Starts plugins that define a starting method."
  [connection plugins]
  (doseq [plugin plugins]
    (if (>= 0.2 (get plugin :otomatik_version 0))
      (when (:on-connect plugin)
        (log/debug "Calling" (get plugin :name "a plugin's") ":on-connect by" (get plugin :author "an uknown author."))
        ((:on-connect plugin) (channel-wrapper/create (:out connection))))
      (when (:init plugin)
        (log/debug "Calling" (get plugin :name "a plugin's") ":init by" (get plugin :author "an uknown author."))
        (if (>= 0.2 (get plugin :otomatik_version 0))
          ((:on-connect plugin) (channel-wrapper/create (:out connection)))
          (go (write-plugin-result (:out connection)
            ((:init plugin) {:nickname (:nickname connection)}))))))))

(defn message-recieved
  [connection plugins packet]
  (doseq [plugin plugins]
    (if (>= 0.2 (get plugin :otomatik_version 0))
      (if (:on-message-recieved plugin)
        ((:on-message-recieved plugin) (channel-wrapper/create (:out connection)) packet) 
      (if (:function plugin)
        (go (write-plugin-result
          (:out connection)
          ((:function plugin) packet))))))))
