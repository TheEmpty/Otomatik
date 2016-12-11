(ns rocks.empty.otomatik.test-irc-client
  (:use [rocks.empty.otomatik.irc-client]
           [clojure.test]))

(deftest test-non-timeout-run-with-timeout
  (let [function (fn [] (Thread/sleep 50) 42)]
    (is (= 42 (run-with-timeout 75 function)))))

(deftest test-non-timeout-run-with-timeout
  (let [function (fn [] (Thread/sleep 2000) 42)]
    (is (= nil (run-with-timeout 5 function)))))
