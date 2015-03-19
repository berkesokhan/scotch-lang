# Java Bindings

Because what good is a JVM language when you can't use Java with it?

Scotch allows for bindings to tie Java code into the language itself. There
is an annotation-driven API provided as well as an implicit form that must be
followed in order to provide Java definitions for Scotch.

## Binding Values

Binding a destructuring pattern:

```
module my.scotch.module

// the Scotch definition
secondElement (_, b) = b
```

```java
// the package name must match the Scotch module name
package my.scotch.module;

import static scotch.runtime.RuntimeSupport.applicable; // creates a single-arg function
import static scotch.runtime.RuntimeSupport.callable;   // creates a thunk
import static scotch.symbol.type.Types.fn;  // creates a single-arg function type
import static scotch.symbol.type.Types.sum; // creates a complex type
import static scotch.symbol.type.Types.var; // creates a type variable

import scotch.symbol.Value;     // marks a method as a declared value
import scotch.symbol.ValueType; // marks a method as a type for a declared value

// This class can be named anything, and you can have multiple classes in this package
// having Scotch definitions.
public class JavaDefinitions {

	// Value methods must return a curried lambda, the method name itself is arbitrary.
	// A thunk should always be ultimately returned after all arguments are applied to
	// a function to ensure lazy evaluation. Everything is a "Callable" until it's
	// needed.
	@Value(memberName = "secondElement")
	public static Applicable<Tuple2<A, B>, B> secondElement() {
		return applicable(tuple -> callable(() -> tuple.call().into((a, b) -> b)));
	}

	// The value type must be described, the method name, again, is arbitrary.
	@ValueType(forMember = "secondElement")
	public static Type secondElement$type() {
		// Describes the type "scotch.data.tuple.(a, b) -> b"
		return fn(sum("scotch.data.tuple.(,)", asList(var("a"), var("b"))), var("b"));
	}
}
```

## Binding Data Types

Data types are declared as classes with their constructors declared as inner
child classes. The supporting values for each constructor must also be defined,
or the constructors will be unusable.

Binding to data types and constructors is quite involved. The Java code below
illustrates what is required to support the equivalent Scotch code:

```
// Scotch declaration for Maybe
module scotch.data.maybe

data Maybe something = Nothing | Just { value something }
```

```java
// Java bindings for equivalent Scotch functionality
package scotch.data.maybe;

import static scotch.runtime.RuntimeSupport.applicable; // creates a single-arg function
import static scotch.runtime.RuntimeSupport.callable;   // creates a thunk
import static scotch.symbol.type.Types.fn;  // creates a single-arg function type
import static scotch.symbol.type.Types.sum; // creates a complex type
import static scotch.symbol.type.Types.var; // creates a type variable

import scotch.symbol.DataConstructor; // marks a class as a constructor
import scotch.symbol.DataType;        // marks a class as a data type
import scotch.symbol.TypeParameter;   // marks a type parameter name
import scotch.symbol.TypeParameters;  // marks a method as providing type descriptors
                                          // for a data types type parameters
import scotch.symbol.Value;           // marks a method as a declared value
import scotch.symbol.ValueType;       // marks a method as a type for a declared value

@DataType(memberName = "Maybe", parameters = {
	@TypeParameter(name = "something"),
})
public abstract class Maybe<Something> {

	// ------------------------ Type Declarations ----------------------------- //

	// this method lists the parameters type descriptors, which are mapped with
	// the list of parameters given with the @DataType annotation
	@TypeParameters
	public static List<Type> parameters() {
		return asList(var("something"));
	}

	// The first data constructor. Note how the data type must be marked in order
	// to tie the relationship between the Maybe type and the Nothing constant
	// constructor. The ordinal must be provided to ensure absolute ordering of
	// declarations.
	@DataConstructor(ordinal = 0, memberName = "Nothing", dataType = "Maybe")
	public static class Nothing<Something> extends Maybe<Something> {

	}

	// The second data constructor.
	@DataConstructor(ordinal = 1, memberName = "Just", dataType = "Maybe")
	public static class Just<Something> extends Maybe<Something> {

		// Fields to hold the argument 'value'
		private final Callable<Something> value;

		// Plain old constructor to take the argument 'value'
		public Just(Callable<Something> value) {
			this.value = value;
		}

		// Getter for the field 'value', plus descriptor with the canon name in
		// Scotch and ordinal to ensure absolute ordering
		@DataField(ordinal = 0, memberName = "value")
		public Callable<Something> getValue() {
			return value;
		}

		// The type descriptor for the field 'value'
		@DataFieldType(forMember = "value")
		public static Type value$type() {
			return var("something");
		}
	}

	// ------------------------ Supporting Values ----------------------------- //

	// static field to hold the singleton instance of the constant Nothing
	private static final Callable<Nothing> NOTHING = callable(Nothing::new);

	// The constant value for Nothing
	@SuppressWarnings("unchecked")
	@Value(memberName = "Nothing")
	public static <Something> Callable<Nothing<Something>> nothing() {
		return (Callable) NOTHING;
	}

	@ValueType(forMember = "Nothing")
	public static Type nothing$type() {
		return sum("scotch.data.maybe.Maybe", var("something"));
	}

	// The constructor for the Just object
	@Value(memberName = "Just")
	public static <Something> Applicable<Something, Just<Something>> just() {
		return applicable(something -> callable(() -> new Just<>(something)));
	}

	@ValueType(forMember = "Just")
	public static Type just$type() {
		return fn(var("something"), sum("scotch.data.maybe.Maybe", var("something")));
	}
}

```
