# Syntax

Scotch borrows a great deal of syntax from Haskell, but also adds some of its
own as well. This guide outlines most of what currently exists in the language.

## Data Types

Data types describe values. They can be declared with one or more *constructors*,
each of which can have zero or more fields. When constructors have fields, the
fields may optionally be named.

```
// A single-constructor data type declaration
data Toast { kind Bread, burnLevel Int }

// A data type declaration with two constructors, one having unnamed fields
data List a = EmptyList | ListNode a (List a)
```

Data types are closed types. This means they cannot be extended like types in
object-oriented languages like Java or C#. The reason for this is because you can
create functions over all known constructors within a data type, which allows for
complete function definitions.

### Constant Constructors

Constant constructors take no arguments and are referenced by name only. They
are effectively singletons.

```
// the constructor Nothing is a constant
data Maybe a = Nothing | Just a
```

### Object Constructors

Objects consist of a collection of named properties. Even when properties are
unnamed, they are named according to ordinal (`_0`, `_1`, etc).

```
// Toast data type declaration
data Toast { kind Bread, burnLevel Int }
```

Objects can be initialized using either unordered property bags or by passing
arguments in the order the properties were declared.

```
// object initializer using property bag
Toast { burnLevel = 2, kind = Sourdough }

// object initializer with positioned arguments
Toast Sourdough 2
```

Tuples are initialized using literal syntax.

```
// tuple literal
tuple = (1, 2, 3)

```

Properties on objects can be access directly using a dot `(.)` but it is preferred
to use destructuring.

```
// object property access with (.)
toast.kind
tuple._1

// object destructuring
kindOf Toast { kind = k } = k
snd (_, b) = b
```

### Generic Type Arguments

Data types may accept type arguments so any type can be stored in their affected
fields. The most common use case for type arguments are collection types:

```
// The built-in list type
data [a] = [] | a : [a]

// The (not yet) built-in bi-map type
data BiMap a v = MapLeaf
               | MapBranch { key a, value v, left BiMap a v, right BiMap a v }
```

## Values

Values are patterns, functions, and expressions.

```
// function literal
\x -> x * x

// expression
2 + 2 * f x

// pattern literal
\(_, b) = b
```

### Declared Values

Values may be declared with a name and referenced from within other values. The
order of declaration is not important.

```
// constant expression
myFavoriteToast = Toast { burnLevel = 2, kind = Sourdough }

// named pattern
snd (_, b) = b

// named function
id = \x -> x

// named capturing pattern
id x = x

// using two declarations
isItTrue? = id myFavoriteToast == myFavoriteToast
```

### Value Signatures

Declared values may be explicitly typed if necessary. Signatures consist of the
name of the declaration followed by a double-colon and the type signature itself.

```
// precede the declaration with the signature
snd :: (a, b) -> b
snd (_, b) = b
```

When two declarations reference each other and both do not have a value signature,
then one must be explicitly typed or the compiler will not be able to infer the types
of either declaration (halting problem).

### Function Application

Functions are curried to accept only single arguments at a time. As a consequence,
the syntax to apply a function to an argument requires only separation by whitespace
or parentheses between the function and argument. This means no superfluous parentheses
or commas!

| Scotch  | C-based Languages |
|---------|-------------------|
| a b     | a(b)              |
| a b c   | a(b, c)           |
| a (b c) | a(b(c))           |
| a b(c)  | a(b, c)           |

[Patterns](#syntax-patterns), [pattern literals](#syntax-patterns-pattern-literals),
and [functions](#syntax-functions) are all curried.

## Lists

Lists in Scotch are persistent, singly-linked lists. They can be created using either
the literal syntax, or using the operator syntax.

```
// literal syntax
[1, 2, 3]

// operator syntax
1:2:3:[]
```

The cons `(:)` operator is used to join a new head to the list on its right. The
`[]` is an empty list.

### Conceptual List Definition

The Scotch list is actually defined in Java for optimization opportunities, but
the Scotch definition would look like so:

```
right infix 5 (:)
data [a] = [] | a : [a]
```

This is only an example. Please note that operator syntax within data type
declarations is unsupported.

### List Patterns

Lists may be easily destructured and matched using the cons operator `(:)` and
empty list `[]`.

```
length [] = 0
length (x:xs) = 1 + length xs
```

## Function Literals

A value that accepts one or more arguments as they are, and returns a value.

```
// an example function
\x -> x * x
```

## Patterns

Similar to a function, except the arguments are matched for value or structure
to decide which values to return.

```
// fibonacci sequence using patterns
fib 0 = 0
fib 1 = 1
fib n = fib (n - 1) + fib (n - 2)

// destructuring 2-tuple
fst (a, _) = a
snd (_, b) = b
```

### Pattern Literals

Like a function, but with destructuring arguments.

```
\(fst, _) -> fst
```
