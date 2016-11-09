# Clojure IRC Bot

Working on writing a generic, plugin-based IRC bot in clojure.

## Usage

Still in active development. You can use `lein run` to get the list of
necessary parameters and their description. Running the bot will create
a very basic bot that just reacts to pings.

## Options

```
  -p, --port PORT          6667  IRC Server Port Number
  -s, --server SERVER            IRC Server hostname
  -n, --nick NICKNAME            IRC Bot's nickname
  -r, --realname REALNAME        IRC Bot's realname
  -c, --channel CHANNEL          IRC Channel to join
```

## TODO

* Some tests
* Plugin structure
* Improve logging
* Make this package a library, rather than client (`lein run` etc).

## License

Copyright © 2016 Mohammad "Empty" El-Abid

Distributed under the Eclipse Public License either version 1.0.

