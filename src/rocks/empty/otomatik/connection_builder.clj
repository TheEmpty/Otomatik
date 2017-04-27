(ns rocks.empty.otomatik.connection-builder
    (:import java.net.Socket)
    (:import javax.net.ssl.SSLSocketFactory)
    (:import java.io.BufferedWriter)
    (:import java.io.BufferedReader)
    (:import java.io.InputStreamReader)
    (:import java.io.OutputStreamWriter)
    (:require [clojure.tools.logging :as log]))

(defn create-socket
  "Creates the socket used to communicate with the server. Given server, port, and optional ssl flag."
  [options]
  (let
    [
     port (if (string? (:port options)) (Long/parseLong (:port options)) (long (:port options)))
     server (str (:server options))
     ssl (get options :ssl)
     ]
    (log/debugf "Trying to connect to %s:%s with ssl = %s" server port (= true ssl))

    ; there is probably a better way to do this...
    (let
      [
        socket (if (= ssl true) (.createSocket (SSLSocketFactory/getDefault) server port) (new Socket server port))
      ]
      (log/debugf "Connected with %s" socket)
      socket)))

(defn create
  "Returns a connection map to the IRC server"
  [options input-channel output-channel]

  (let
    [
      socket (create-socket options)
      outputBuffer (new BufferedWriter (new OutputStreamWriter (.getOutputStream socket)))
      inputBuffer (new BufferedReader (new InputStreamReader (.getInputStream socket)))
      nickname (ref (:nickname options))
    ]

    {
     :in input-channel
     :out output-channel
     :reader inputBuffer
     :writer outputBuffer
     :socket socket
     :nickname nickname
    }))