; Rules: https://tools.ietf.org/html/rfc2812#section-2.3.1

(ns rocks.empty.clojure.irc.irc-commands)

; TODO: rename, work a bit better with options, prefix, etc.
; Maybe move out of this file and into a more client friendly one
(defn irc-command
  [writer, & args]
  (.write writer (str (clojure.string/join " " args) "\r\n"))
  (.flush writer))

; TODO: rename this or move into parse-prefix
(defn parse-user
  "Returns the nickname, realname, and host of a given IRC user string"
  [line]
  (def matcher (re-matcher #"^:(.+?)(\!.+?)?(@.+?)?$" line))
  (def results (re-find matcher))
  ; TODO: drop the ! and @ in results 2 and 3 respectively
  (if (and (= nil (nth results 2)) (= nil (nth results 3)))
    {:server (nth results 1)}
    {:nickname (nth results 1) :realname (nth results 2) :host (nth results 3)}))

(defn parse-params [params]
  "Parses the parameter part of an IRC message"
  (if (= params nil) nil
    (do
      (def trimmed (clojure.string/trim params))
      (def trailing-split (clojure.string/split trimmed #":" 2))
      (def bare (clojure.string/split (nth trailing-split 0) #" "))

      (if (= 1 (count trailing-split))
        bare
        (if (= "" (nth trailing-split 0))
          (list (nth trailing-split 1))
          (concat bare (list (nth trailing-split 1))))))))

(defn parse-prefix [prefix]
  "Parses the prefix part of an IRC message"
  (if (= prefix nil) nil
    (do
      (def trimmed (clojure.string/trim prefix))
      (parse-user trimmed))))

(defn parse-message [line]
  (def parsed (re-find (re-matcher #"^(:.+? )?(.+?)( .+?)?$" line)))
  (def prefix (parse-prefix (nth parsed 1)))
  (def command (nth parsed 2))
  (def params (parse-params (nth parsed 3)))

  {:prefix prefix :command command :params params})
