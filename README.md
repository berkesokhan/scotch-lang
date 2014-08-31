# Scotch Programming Language [![Build Status](https://secure.travis-ci.org/lmcgrath/scotch-lang.png)](https://travis-ci.org/lmcgrath/scotch-lang/)

### Building

**Requirements**
* [Maven 3](http://maven.apache.org/)
* [Java JDK 8](https://jdk8.java.net/download.html)

To build the binary distribution of Scotch, simply run the following command:

```
$ mvn package
```

This will create a zip file at `target/scotch-lang-{VERSION}-bin.zip`. Expand this file and add the `bin` folder to your path.

### Hello World

Create the file `hello.scotch` with the contents:

```
module hello

main = print "Hello, World!"
```

Run it with this command:

```
$ scotch hello
```
