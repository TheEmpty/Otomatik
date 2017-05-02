(ns rocks.empty.otomatik.default-plugins.channel-watcher
  (:require [clojure.tools.logging :as log]))

;; TODO: what if the bot parts the room, should delete the room
;; TODO: refactor, this was thrown together more as proof of concept.
;; TODO: support: +v, +o, -o; should be: v, currently: nil
;; NOTE: many clients don't seem to even support the above use case (if you quit or part between but otherwise they will)

(defn get-role
  [user]
  (case (subs user 0 1)
    "@" ['op]
    "%" ['hop]
    "+" ['voice]
    []))

(defn get-role-from-mode
  [mode]
  (case mode
    "+o" ['op]
    "+h" ['hop]
    "+v" ['voice]
    []))

(defn get-nick
  [user]
  (clojure.string/replace user #"^(%|@|\+)" ""))

(defn parse-user
  [user]
  {:nickname (get-nick user) :role (get-role user)})

(defn parse-users
  [users]
  (map parse-user users))

(defn add-users
  [chans users room]
  (locking chans
    (dosync
      (ref-set chans
        (conj
          @chans
          {
            room
            (distinct
              (concat (get @chans room []) users))
          })))))

(defn remove-user
  [chans user room]
  (locking chans
    (dosync
      (ref-set chans
        (conj @chans
          { room (remove #(= (:nickname %1) user) (get @chans room)) })))))

(defn remove-from-all
  [chans user]
  (locking chans
    (doseq [[room users] @chans]
      (remove-user chans user room))))

(defn change-nick-in-room
  [chans room from to]
  (locking chans
    (dosync
      (ref-set chans
        (conj @chans
          {
            room
            (map (fn [user]
              (if (= from (:nickname user))
                {:nickname to :role (:role user)}
                user)) (get @chans room))
          })))))

(defn changed-nick
  [chans from to]
  (locking chans
    (doseq [[room users] @chans]
      (change-nick-in-room chans room from to))))

; This needs to the first plugins
; because of channel joiner for example
(defn registration
  [chans]
  {
    :name "Channel Watcher"
    :author "@theempty on GitHub"
    :otomatik_version 0.2

    :on-message-recieved
      (fn [writer packet state]

        ; /NAMES
        (when (= "353" (:command packet))
          (let [
            chan (nth (:params packet) 2)
            users (parse-users (clojure.string/split (nth (nth (split-at 3 (:params packet)) 1) 0) #" "))
            ]
            (log/tracef "Chan %s has users: %s" chan users)
            (add-users chans users chan)))
        
        (when (= "JOIN" (:command packet))
          (let [
            nick (nth (:prefix packet) 0)
            room (nth (:params packet) 0)
            ]
            (log/tracef "%s has joined %s." nick room)
            (add-users chans [(parse-user nick)] room)))
            
        (when (= "PART" (:command packet))
          (let [
            nick (nth (:prefix packet) 0)
            room (nth (:params packet) 0)
            ]
            (log/tracef "%s has left %s." nick room)
            (remove-user chans nick room)))
              
        (when (= "QUIT" (:command packet))
          (let [nick (nth (:prefix packet) 0)]
            (log/tracef "%s has quit." nick)
            (remove-from-all chans nick)))

        (when (= "NICK" (:command packet))
          (let [from (get (:prefix packet) 0) to (first (:params packet))]
            (log/tracef "%s is now known as %s." from to)
            (changed-nick chans from to)))

        (when (= "MODE" (:command packet))
          (let [
            room (get (:params packet) 0)
            mode (get (:params packet) 1)
            nick (get (:params packet) 2)
            ]
            (when (and (not (= nil room)) (not (= nil mode)) (not (= nil nick)))
              (log/tracef "%s just got %s in %s" nick mode room)
              (locking chans
                (dosync
                  (ref-set chans
                    (conj @chans
                      {room (map (fn [user]
                        (if (= nick (:nickname user))
                          {:nickname nick :role (get-role-from-mode mode)}
                          user)) (get @chans room))})))))))

        (when (= "KICK" (:command packet))
          (let [
            room (get (:params packet) 0)
            nick (get (:params packet) 1)
            ]
            (log/tracef "%s just got kicked from %s." nick room)
            (remove-user chans nick room))))
  })
