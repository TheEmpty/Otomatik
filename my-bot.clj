; Just testing some ideas
(def options {
 :nickname "EmptyBot"
 :realname "Empty"
 :channel "#emptytest"
 :server "irc.freenode.com"
 :plugins [rocks.empty.clojure.irc.plugins.timezones]
})

(rocks.empty.clojure.irc/bot options)
