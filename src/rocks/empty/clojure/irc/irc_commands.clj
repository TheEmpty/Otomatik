; IRC Rules: https://tools.ietf.org/html/rfc2812#section-2.3.1

(ns rocks.empty.clojure.irc.irc-commands)

; TODO: rename, work a bit better with options, prefix, etc.
; Maybe move out of this file and into a more client friendly one
(defn irc-command
  [writer, & args]
  (locking writer
    (.write writer (str (clojure.string/join " " args) "\r\n"))
    (.flush writer)))

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
