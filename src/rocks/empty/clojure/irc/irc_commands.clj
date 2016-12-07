; IRC Rules: https://tools.ietf.org/html/rfc2812#section-2.3.1

(ns rocks.empty.clojure.irc.irc-commands)

(defn irc-command
  "Deprecated, please use 'write'."
  [writer, & args]
  (locking writer
    (.write writer (str (clojure.string/join " " args) "\r\n"))
    (.flush writer)))

(defn build-options-string
  [options]
  (if (some #(.contains %1 " ") (butlast options))
    nil
    (clojure.string/join " " (concat (butlast options) [(str ":" (last options))]))))

(defn build-message
  "Builds a string for the given command and options."
  [data]
  (let [
        ; TODO: prefix (build-irc-string (:prefix data))
        command (:command data)
        options (build-options-string (:options data))
        ]
    (if (= nil command)
      nil
      (clojure.string/join " " (remove nil? (list command options))))))

(defn write
  "Builds and writes an IRC command to the server. First option should
  be the writer and the second options a map that is passed into build-message."
  [writer, data]
  (let [message (build-message data)]
    (if-not (= message nil)
      (locking writer
        (.write writer (str message "\r\n"))
        (.flush writer)))))

(defn parse-user-or-server
  "Returns the nickname, realname, and/or host of a given IRC user string"
  [prefix]
  (let [results (re-find (re-matcher #"^:(.+?)(\!.+?)?(@.+?)?$" prefix))]
    (if (and (= nil (nth results 2)) (= nil (nth results 3)))
      [(nth results 1)]
      [(nth results 1) (subs (nth results 2) 1) (subs (nth results 3) 1)])))

(defn parse-params [params]
  "Parses the parameter part of an IRC message"
  (if (= params nil) nil
    (let
      [
        trimmed (clojure.string/trim params)
        trailing-split (clojure.string/split trimmed #":" 2)
        bare (clojure.string/split (nth trailing-split 0) #" ")
      ]

      (if (= 1 (count trailing-split))
        bare
        (if (= "" (nth trailing-split 0))
          (list (nth trailing-split 1))
          (concat bare (list (nth trailing-split 1))))))))

(defn parse-prefix [prefix]
  "Parses the prefix part of an IRC message"
  (if (= prefix nil) nil
    (parse-user-or-server (clojure.string/trim prefix))))

(defn parse-message [line]
  "Parses a raw message from the IRC server."
  (let [parsed (re-find (re-matcher #"^(:.+? )?(.+?)( .+?)?$" line))]
    {
      :prefix (parse-prefix (nth parsed 1))
      :command (nth parsed 2)
      :params (parse-params (nth parsed 3))
    }))
