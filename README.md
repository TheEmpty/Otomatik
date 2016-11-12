# Clojure IRC Bot

An IRC bot in Clojure! Designed to be modular and plugin-based.

## Usage

[![Clojars Project](http://clojars.org/irc-bot/latest-version.svg)](http://clojars.org/irc-bot)

For an example client, check out [TheEmpty/Clojure-IRC-Bot](https://github.com/TheEmpty/Clojure-IRC-Bot).


### Starting your client
You'll need to `(:require [rocks.empty.clojure.irc.irc-client :as irc-client])` to call
the entry point for your bot, `(irc-client/bot options)`. For options, you are required
to give `:nickname`, `:realname`, `:server`, `:port`, and a list of `:plugins`.
To make your client join a channel automatically, add
`(:require [rocks.empty.clojure.irc.default-plugins.channel-joiner :as channel-joiner])`
and then you can add
`(channel-joiner/registration [ "#my-channel" ])` to your list of plugins to have your bot join.


### Writing your own plugins
Plugins passed to `irc-client/bot` are assumed to be a map with the key `:author`
and additional keys mapped to a function for given cases (see below).

```clojure
{
  :author "How to get ahold of me."
  :init (fn [connection] (my-init connection)) ; called when the bot connects to the server.
  :handle (fn [packet] (my-handle packet)) ; called when a message is recieved from the server.
}
```

The packet and connection have the following keys.

```clojure
packet {
  :raw ; the raw line read from the server.
  :message
  :connection
}

message {
  :prefix  ; the IRC prefix, {:server} or {:nickname :realname :host}.
  :command ; the IRC command
  :params  ; parameters to the IRC command.
}

connection { ; NOTE: use (locking obj) whenever you using these!
  :reader ; java.io.BufferedReader
  :writer ; java.io.BufferedWritter
  :socket ; java.net.Socket
}

```


## Future Improvements

* Improve irc-commands.
* Test suite.
* Improve logging.
* Improve documentation.

## License

Copyright © 2016 Mohammad "Empty" El-Abid

Distributed under the Eclipse Public License either version 1.0.
