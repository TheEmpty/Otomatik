(ns rocks.empty.clojure.irc.bot-core
    (:gen-class)
    (:require [clojure.tools.cli :refer [parse-opts]])
    (:require [rocks.empty.clojure.irc.irc-commands :as irc-commands])
  )

(def required-options #{:port :server :nick :realname :channel})

(def cli-options
  [["-p" "--port PORT" "IRC Server Port Number"
   :default 6667
   :parse-fn #(Integer/parseInt %)]
  ["-s" "--server SERVER" "IRC Server hostname"]
  ["-n" "--nick NICKNAME" "IRC Bot's nickname"]
  ["-r" "--realname REALNAME" "IRC Bot's realname"]
  ["-c" "--channel CHANNEL" "IRC Channel to join"]])

(defn -main
  [& args]
  (def opts (parse-opts args cli-options))
  (def options (:options opts))

  (when (not (= nil (:errors opts)))
    (println (:errors opts))
    (println (:summary opts))
    (System/exit -1))

  (when (not-every? options required-options)
    (println (str "Failed to find required arguments: " required-options " in " options))
    (println (:summary opts))
    (System/exit -1))

  (def server (:server options))
  (def nickname (:nick options))
  (def channel (:channel options))
  (def login (:realname options))
  (def port (:port options))
  (def connection (irc-commands/connect server port nickname login channel))
  (irc-commands/main-loop connection))

