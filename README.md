# konu-notes

## Development

try `lein eclipse`or `lein idea`.

## Running

To start a web server for the application, run:

    lein ring server

or run ```main-``` from ```konu-notes.server``` namespace in repl

## Packaking

    lein do clean, uberjar

## Running

### Standalone

    java -jar target/compojure-intro-0.1.0-SNAPSHOT-standalone.jar

### Warred

    jetty target/compojure-intro-0.1.0-SNAPSHOT-standalone.jar

## License

Copyright &copy; 2015 Konu

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
