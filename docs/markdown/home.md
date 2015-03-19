# Scotch Language Spec

"Thought of it while drinking"

Scotch arose out of frustration from years of working with Java and other
C/C++-like programming languages. Specifically, Scotch sets out to alleviate:

- Verbose, unnecessary syntax
- Lengthy type signatures
- Difficulty composing functions together even when functions supported as first-class citizens
- Long, hard to read conditionals
- Subtype polymorphism
- Abusing instanceof and typeof operators
- So many curly braces and parentheses and semicolons!
- Mutability by default
- Side-effect-driven state changes

These problems are addressed by providing the following features:

- Terse syntax with little in the way of reserved keywords and operators
- Type inferencing with little need to provide type annotations
- Short, easy to understand type signatures
- First-class functions and function currying
- Pattern matching for easy conditionals and destructuring of values
- Closed types coupled with pattern matching alleviating any need for instanceof and typeof
- Ad-hoc polymorphism support by using [Haskell](http://www.haskell.org)-style [type classes](http://learnyouahaskell.com/types-and-typeclasses)
- First-class functions to support function composition
- Whitespace-separated functions and arguments in place of parentheses and commas
- [Off-side rule](http://en.wikipedia.org/wiki/Off-side_rule) syntax to remove need for curly braces and semicolons
- All values are immutable, special references to values may be mutable (as in [Clojure](http://blog.jayfields.com/2011/04/clojure-state-management.html))

Dynamic typing, while popular among newer languages like [Go](https://golang.org/),
was not a goal due to the huge safety net provided by a strong type system. Scotch
is both very strongly and statically typed. This ensures a high degree of code safety
and correctness.

## Inspiration

Scotch takes great inspiration from Haskell, but does not attempt to be a Haskell
on the JVM (check out [Frege](https://github.com/Frege/frege) for that.) There are
several reasons for this, primarily to maintain accessibility to the average developer,
but especially to allow breakage from Haskell in cases where Haskell idioms
[aren't directly supported](http://fregepl.blogspot.com/2013/03/adding-concurrency-to-frege-part-ii.html)
within the JVM and must be written differently.
