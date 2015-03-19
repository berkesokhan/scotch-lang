# Quickstart

Scotch is still in early development, so it's very feature light. At the very least
you can get a good feel for what it's capable of.

To date, all that the compiler is capable of doing is single-file compilation.
It will run a `main` function defined within the file, and print the result to
the console. This is useful for quick iteration and debugging, and currently
used just to poke at the compiler to see what it's capable of doing given its
current state in development.

## Building The Compiler

Currently Scotch must be built in order to be used. [Clone the repo](https://github.com/lmcgrath/scotch-lang)
and run the following command within the repo folder:

```
$ ./gradlew distZip
```

This will create a zip distribution in `build/distributions/scotch-${VERSION}.zip`. Simply
unzip this file, then add the `bin` folder to your `PATH`.

## Hello World!

To run Hello World! create the following file:

```
// hello.scotch
module hello

main = "Hello World!"
```

Then in the same directory as your file, execute the following:

```
$ scotch -m hello
```

And you should see the following output:

```
main = Hello World!
```

## Running 2 + 2

In order to do things that are more complicated, many different imports are required.

A simple operation, 2 + 2, requires an import:

```
// hello.scotch
module hello
import scotch.data.num

main = 2 + 2
```

The module `scotch.data.num` brings in numeric operations `+`, `-`, `*` and a few
others that allow basic arithmetic. If you run this module you'll see the following:

```
$ scotch -m hello
$ main = 4
```

## More Complicated Examples

The test code found [here](https://github.com/lmcgrath/scotch-lang/blob/master/src/test/java/scotch/compiler/steps/BytecodeGeneratorTest.java)
will show some more complicated examples you can run.

If you wish to make your module names more complicated, nest them within folders
as you would Java packages. Otherwise, feel free to continue using `hello` because
it's super easy.
