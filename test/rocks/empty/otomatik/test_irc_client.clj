(ns rocks.empty.otomatik.test-irc-client
  (:use [clojure.test])
  (:require [rocks.empty.otomatik.irc-client :as client])
  (:require [clojure.core.async
    :as a
    :refer [>! <! >!! <!! go go-loop chan buffer close! thread alts! alts!! timeout]]))

(deftest test-write-plugin-result-with-nil
  (let [output (chan)]
    (client/write-plugin-result output nil)
    (<!! (timeout 3))
    (close! output)
    (is (= nil (<!! output)))))

(deftest test-write-plugin-result-with-str
  (let [output (chan) message "PRIVMSG TheEmpty :Testing 1 2 3."]
    (client/write-plugin-result output message)
    (<!! (timeout 3))
    (close! output)
    (is (= message (<!! output)))))

(deftest test-write-plugin-result-with-seq
  (let [output (chan) message1 "PRIVMSG TheEmpty :Well," message2 "PRIVMSG TheEmpty :No"]
    (client/write-plugin-result output (seq [message1 message2]))
    (<!! (timeout 3))
    (close! output)
    (is (= message1 (<!! output)))
    (is (= message2 (<!! output)))))
