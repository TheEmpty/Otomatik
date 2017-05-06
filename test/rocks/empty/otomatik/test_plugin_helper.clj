(ns rocks.empty.otomatik.test-plugin-helper
  (:use [clojure.test])
  (:require [rocks.empty.otomatik.plugin-helper :as plugin-helper])
  (:require [clojure.core.async
    :as a
    :refer [>! <! >!! <!! go go-loop chan buffer close! thread alts! alts!! timeout]]))

; TODO: test with a plugin that fucks shit up

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

(deftest test-build-state-map-adds-nickname
  (let [connection {:nickname "Tester"} state {:something "The Other"}]
    (let [result (plugin-helper/build-state-map connection state)]
        (is (= (get result :nickname) "Tester"))
        (is (= (get result :something) "The Other")))))

(deftest calls-plugins-on-connect
    (let [
            output (chan)
            connection {:out output}
            state {}
            plugin {:on-connect (fn [write state] (write "MyUniqueName"))}
         ]
        (plugin-helper/init-plugin-zero-dot-two connection state plugin)
        (<!! (timeout 3))
        (close! output)
        (is (= "MyUniqueName" (<!! output)))))

(deftest doesnt-break-if-on-connect-breaks
    (let [
            output (chan)
            connection {:out output}
            state {}
            plugin {:on-connect (fn [write state] (write (/ 5 0)))}
         ]
        (plugin-helper/init-plugin-zero-dot-two connection state plugin)
        (<!! (timeout 3))
        (close! output)))

(deftest calls-plugins-init
    (let [
        output (chan)
        connection {:out output}
        state {}
        plugin {:init (fn [state] "MyUniqueName")}
        ]
        (plugin-helper/init-plugin-original connection state plugin)
        (<!! (timeout 3))
        (close! output)
        (is (= "MyUniqueName" (<!! output)))))

(deftest doesnt-break-if-init-breaks
    (let [
        output (chan)
        connection {:out output}
        state {}
        plugin {:init (fn [state] (/ 5 0))}
        ]
        (plugin-helper/init-plugin-original connection state plugin)
        (<!! (timeout 3))
        (close! output)))

(deftest calls-on-message-recieved
    (let [
        output (chan)
        connection {:out output}
        state {:state true}
        packet {:message {:message true} :raw "whatever"}
        plugin {:on-message-recieved
            (fn [writefn message state] (do
                (println "Message = " message)
                (is (= true (:state state)))
                (is (= true (:message message)))
                (is (= "whatever" (:raw message)))
                (writefn "Not broken")))}
        ]
        (plugin-helper/message-recieved-zero-dot-two connection plugin state packet)
        (<!! (timeout 3))
        (close! output)
        (is (= "Not broken" (<!! output)))))

(deftest doesnt-break-if-on-message-recieved-breaks
    (let [
        output (chan)
        connection {:out output}
        state {:state true}
        packet {:message {:message true} :raw "whatever"}
        plugin {:on-message-recieved (fn [writefn message state] (writefn (/ 5 0)))}
        ]
        (plugin-helper/message-recieved-zero-dot-two connection plugin state packet)
        (<!! (timeout 3))
        (close! output)))

(deftest calls-function-for-old-plugins
    ; I'm so sorry I named it function...
    ; It was a proof of concept.
    (let [
        output (chan)
        connection {:out output}
        packet {:packet true}
        plugin {:function (fn [packet] (do (is (= true (:packet packet))) "Yup"))}
        ]
        (plugin-helper/message-recieved-original connection plugin packet)
        (<!! (timeout 3))
        (close! output)
        (is (= "Yup" (<!! output)))))

(deftest doesnt-break-if-function-for-old-plugins-breaks
    (let [
        output (chan)
        connection {:out output}
        packet {:packet true}
        plugin {:function (fn [packet] (/ 5 0))}
        ]
        (plugin-helper/message-recieved-original connection plugin packet)
        (<!! (timeout 3))
        (close! output)))
