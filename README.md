# Otomatik

An IRC bot in Clojure. Written in my free time as an introduction to Clojure.

## Tenants

### Dead Simple Configuration
A non-coder should be able to safely and easily setup an
Otomatik IRC Bot with plugins.

### Minimal Core and Plugin Based
Plugins should be the base for all features. Even core
functionality should be a plugin (PING/PONG), but otomatikly included.

### Easily Written Plugins
Someone that is familiar with Clojure (or a supported JVM language)
should be able to easily pickup the Otomatik framework and easily
write a plugin to perform basic tasks.

## Future Improvements

* A full test suite.
* Full documentation suite.
* A concrete structure for plugins.

## Known Issues

* Failure to close sockets and exit when network is interrupted.

## License

Copyright © 2016-2017 Mohammad "Empty" El-Abid

Distributed under the MIT License.
