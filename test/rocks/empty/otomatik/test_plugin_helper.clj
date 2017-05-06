(ns rocks.empty.otomatik.test-plugin-helper
  (:use [clojure.test])
  (:require [rocks.empty.otomatik.plugin-helper :as plugin-helper])
  (:require [clojure.core.async
    :as a
    :refer [>! <! >!! <!! go go-loop chan buffer close! thread alts! alts!! timeout]]))

(deftest test-write-plugin-result-with-nil
  (let [output (chan)]
    (plugin-helper/write-plugin-result output nil)
    (<!! (timeout 3))
    (close! output)
    (is (= nil (<!! output)))))

(deftest test-write-plugin-result-with-str
  (let [output (chan) message "PRIVMSG TheEmpty :Testing 1 2 3."]
    (plugin-helper/write-plugin-result output message)
    (<!! (timeout 3))
    (close! output)
    (is (= message (<!! output)))))

(deftest test-write-plugin-result-with-seq
  (let [output (chan) message1 "PRIVMSG TheEmpty :Well," message2 "PRIVMSG TheEmpty :No"]
    (plugin-helper/write-plugin-result output [message1 message2])
    (<!! (timeout 3))
    (close! output)
    ; Order is not guranteed, so load up and then check.
    (let [result1 (<!! output) result2 (<!! output) results #{result1 result2}]
        (is (contains? results message1))
        (is (contains? results message2)))))
