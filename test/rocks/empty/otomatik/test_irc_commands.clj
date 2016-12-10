(ns rocks.empty.otomatik.test-irc-commands
  (:use [rocks.empty.otomatik.irc-commands]
           [clojure.test]))

(deftest test-build-options
  (is (= "TOPIC :Empty Bot" (build-options-string (list "TOPIC" "Empty Bot"))))
  (is (= nil (build-options-string (list "I don't know what I'm doing" "But, I know how to do it.")))))

(deftest test-build-message
  (is (= "TOPIC :Empty Bot" (build-message {:command "TOPIC" :options ["Empty Bot"]})))
  (is (= nil (build-message {:options ["Empty Bot"]}))))
