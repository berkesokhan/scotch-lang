# Scotch Base Library

## List

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
