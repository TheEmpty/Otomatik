(ns rocks.empty.otomatik.test-channel-wrapper
  (:use [clojure.test])
  (:require [rocks.empty.otomatik.channel-wrapper :as channel-wrapper])
  (:require [clojure.core.async
    :as a
    :refer [<!! chan close! timeout]]))

(deftest it-works
  (let [output (chan) wrapper (channel-wrapper/create output)]
    (wrapper "otomatik")
    (<!! (timeout 3))
    (close! output)
    (is (= "otomatik" (<!! output)))))
