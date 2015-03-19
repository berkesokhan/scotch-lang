# Syntax

Scotch borrows a great deal of syntax from Haskell, but also adds some of its
own as well. This guide outlines most of what currently exists in the language.

## Identifiers

Scotch is extraordinarily liberal in what it accepts as an identifier. Identifiers
can contain numbers, hyphens, exclamation points, weird symbols, and end in single
quotes!

Here is a short list of some valid identifiers:

- `EggsAndBacon`
- `ham-n-eggs`
- `burnedToast`
- `really?`
- `yes!`
- `what?!`
- `2+2` *yes, 2+2 is really a single identifier!*
- `prime'`
- `doublePrime''`
- `12@9--$'`

### Naming Conventions

| Element         | Naming Convention |
|-----------------|-------------------|
| Type Name       | UpperCamel        |
| Declared Value  | lowerCamel or operator symbol |
| Variables       | lowerCamel        |
| Type Variable   | single lower-case letter\* |
| Constructor     | UpperCamel        |
| Object Property | lowerCamel        |
| Operator        | Non-alpha symbols |
| Module Name     | Valid Java package name |

\* Type variables are normally single letters, though full names can be used for clarity

## Function Application

Functions are curried to accept only single arguments at a time. As a consequence,
the syntax to apply a function to an argument requires only separation by whitespace
or parentheses between the function and argument. This means no superfluous parentheses
or commas!

| Scotch   | C-based Languages |
|----------|-------------------|
| fn b     | fn(b)             |
| fn b c   | fn(b, c)          |
| fn (b c) | fn(b(c))          |
| fn b(c)  | fn(b, c)          |

[Functions](#syntax-values-function-literals) and [patterns](#syntax-values-patterns) are both curried.

## Declared Values

Values may be declared with a name and referenced from within other values. The
order of declaration is not important.

```
// constant expression consisting of Toast object
myFavoriteToast = Toast { burnLevel = 2, kind = Sourdough }

// capturing pattern
identity x = x

// capturing function literal
identity = \x -> x

// destructuring pattern
secondElement (_, b) = b

// destructuring function literal
secondElement = \(_, b) -> b

// constant expression using two declarations
isItTrue? = identity myFavoriteToast == myFavoriteToast
```

## Type Signatures

Declared values may be explicitly typed if necessary. This allows enforcement of
interfaces and compiler support in cases when types are undecidable. Type signatures
consist of the name of the declaration followed by a double-colon and the type
signature itself.

```
// precede the declaration with the signature
secondElement :: (a, b) -> b
secondElement (_, b) = b
```

When two declarations reference each other and both do not have a value signature,
then one must be explicitly typed or the compiler will not be able to infer the types
of either declaration (halting problem).

```
// these two values' types can't be determined because they depend on each other
fn a = fn2 a
fn2 b = fn b
```

## Function Literals

Function literals start with a backslash because it looks like a lambda (as in
Lambda Calculus) then list the arguments of the function. An arrow separates
the arguments from the body of the function. Function literals may be placed
anywhere a function value is expected.

```
// an example function which squares its argument
\x -> x * x

// grabbing the first element of a 2-tuple and ignoring the second
\(firstElement, _) -> firstElement

// ignoring the only argument of a function
\_ -> 2
```

## Patterns

Patterns function similarly to functions in that they accept arguments and return
values. The key difference is that they also function as conditionals by matching
on values and can pull apart objects (destructuring).

```
// fibonacci sequence using value matching
nthFibonacci 0 = 0
nthFibonacci 1 = 1
nthFibonacci n = fib (n - 1) + fib (n - 2)

// destructuring 2-tuple objects,
// capturing properties with variable names and ignoring properties with underscores
firstElement (a, _) = a
secondElement (_, b) = b
```

## Data Type Definitions

A data type in Scotch is a single umbrella over a closed number of variants, called
"constructors". Data types are declared using the `data` keyword, followed by the
*capitalized* type name, then an equals sign and a pipe-separated list of constructors.

### Creating Data Types

Data types require a list of type constructors. Type constructors create objects
of their parent type. To create a constructor, provide a *capitalized* name, then
optionally follow it with properties enclosed in curly braces. If there are no
properties, the constructor creates constants, otherwise it will produce objects.

Note that all data type names and constructor names must be capitalized. Data
types will fail to compile otherwise. Conversely, the first letter of property
names must be lower case.

As an example, here is a [Binary Tree Map](http://pages.cs.wisc.edu/~skrentny/cs367-common/readings/Binary-Search-Trees/):

```
// A data type with both a constant constructor and a complex constructor.

data InventoryTree = InventoryLeaf // this constructor has no properties and thus is a constant
                   | InventoryNode { item String, // property name 'item' is followed by type 'String'
                                     count Int,
                                     leftBranch InventoryTree,
                                     rightBranch InventoryTree }
```

The `InventoryNode` constructor is particularly verbose with its property definitions.
In its particular case, this helps clarify what its objects will contain. However,
there are cases where objects are so simple you may not want to name their properties:

```
// A data type describing a singly-linked list of sausage links
data SausageLinks = NoMoreSausage
                  | SausageLink Sausage SausageLinks // No properties after the constructor name, just a list of types
```

When a constructor has its properties declared as a list of types, each property
is implicitly named in the order it was declared as `_0`, `_1`, etc. and may be
referenced as any other property.

## Object Constructors

Objects are instantiated from [complex constructors](#syntax-data-type-definitions-creating-data-types).
There are two ways that objects can be instantiated:

### Property Bag Instantiation

Property bag instantiation requires providing a list of properties enclosed in
curly braces after the name of the constructor to create a new object. The properties
within the property bag can be in any order.

```
// The data type definitions
data Bread = Sourdough | Pumpernickle | Rye
data Toast { kind Bread, burnLevel Int }

// instantiating Toast
myFavoriteToast = Toast { burnLevel = 2, kind = Sourdough } // properties in any order! :)

```

### Positioned Instantiation

Positioned instantiation is used with objects that have unnamed or positioned
properties. Even objects with named properties can be instantiated in this manner
as long as their arguments are provided in the order they were declared.

```
// The data type definitions
data Bread = Sourdough | Pumpernickle | Rye
data Toast { kind Bread, burnLevel Int }

data Sausage = Sausage
data SausageLinks = NoMoreSausage
                  | SausageLink Sausage SausageLinks

// instantiating constructor with unnamed properties
threeSausages = SausageLink Sausage (SausageLink Sausage (SausageLink Sausage NoMoreSausage))

// instantiating constructor with named properties without property names
myLeaseFavoriteToast = Toast Pumpernickle 5
```

## Constant Constructors

Constants are [constructors](#syntax-data-type-definitions-creating-data-types)
which have no arguments. They are singleton values and are declared in their
respective data types with no properties.

```
// A complex data type consisting of three constant constructors
data Color = Red | Blue | Green

// A complex data type having both a constant constructor and a complex constructor
data SausageLinks = NoMoreSausage // a constant constructor
                  | SausageLink Sausage SausageLinks // a complex constructor
```

Using constants in a value is really easy. You just reference it by name.

```
// A list of colors
colors = [Red, Blue, Green]

// two sausage links
twoSausages = SausageLink Sausage (SausageLink Sausage NoMoreSausage)
```
