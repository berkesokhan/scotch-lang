package scotch.compiler.syntax;

import static java.util.stream.Collectors.joining;
import static scotch.util.StringUtil.quote;
import static scotch.util.StringUtil.stringify;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import com.google.common.collect.ImmutableList;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.TypeClassDescriptor;
import scotch.compiler.symbol.TypeInstanceDescriptor;
import scotch.compiler.symbol.Unification;
import scotch.compiler.text.SourceRange;

public abstract class SyntaxError {

    public static SyntaxError ambiguousTypeInstance(TypeClassDescriptor typeClass, List<Type> parameters, Set<TypeInstanceDescriptor> typeInstances, SourceRange location) {
        return new AmbiguousTypeInstanceError(typeClass, parameters, typeInstances, location);
    }

    public static SyntaxError parseError(String description, SourceRange location) {
        return new ParseError(description, location);
    }

    public static SyntaxError symbolNotFound(Symbol symbol, SourceRange location) {
        return new SymbolNotFoundError(symbol, location);
    }

    public static SyntaxError typeError(Unification unification, SourceRange location) {
        return new TypeError(unification, location);
    }

    public static SyntaxError typeInstanceNotFound(TypeClassDescriptor typeClass, List<Type> parameters, SourceRange location) {
        return new TypeInstanceNotFoundError(typeClass, parameters, location);
    }

    private SyntaxError() {
        // intentionally empty
    }

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    public abstract String prettyPrint();

    @Override
    public abstract String toString();

    public static class AmbiguousTypeInstanceError extends SyntaxError {

        private final TypeClassDescriptor         typeClass;
        private final List<Type>                  parameters;
        private final Set<TypeInstanceDescriptor> typeInstances;
        private final SourceRange                 location;

        public AmbiguousTypeInstanceError(TypeClassDescriptor typeClass, List<Type> parameters, Set<TypeInstanceDescriptor> typeInstances, SourceRange location) {
            this.typeClass = typeClass;
            this.parameters = parameters;
            this.typeInstances = typeInstances;
            this.location = location;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof AmbiguousTypeInstanceError) {
                AmbiguousTypeInstanceError other = (AmbiguousTypeInstanceError) o;
                return Objects.equals(typeClass, other.typeClass)
                    && Objects.equals(parameters, other.parameters)
                    && Objects.equals(typeInstances, other.typeInstances)
                    && Objects.equals(location, other.location);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(typeClass, parameters, typeInstances);
        }

        @Override
        public String prettyPrint() {
            return "Ambiguous instance of " + quote(typeClass.getSymbol().getCanonicalName())
                + " for parameters [" + parameters.stream().map(Type::prettyPrint).collect(joining(", ")) + "];"
                + " instances found in modules [" + typeInstances.stream().map(TypeInstanceDescriptor::getModuleName).collect(joining(", ")) + "]"
                + " " + location.prettyPrint();
        }

        @Override
        public String toString() {
            throw new UnsupportedOperationException(); // TODO
        }
    }

    public static class ParseError extends SyntaxError {

        private final String      description;
        private final SourceRange sourceRange;

        private ParseError(String description, SourceRange sourceRange) {
            this.description = description;
            this.sourceRange = sourceRange;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof ParseError) {
                ParseError other = (ParseError) o;
                return Objects.equals(description, other.description)
                    && Objects.equals(sourceRange, other.sourceRange);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(description, sourceRange);
        }

        @Override
        public String prettyPrint() {
            return description + " " + sourceRange.prettyPrint();
        }

        @Override
        public String toString() {
            return stringify(this) + "(description=" + quote(description) + ")";
        }
    }

    public static class SymbolNotFoundError extends SyntaxError {

        private final Symbol      symbol;
        private final SourceRange sourceRange;

        private SymbolNotFoundError(Symbol symbol, SourceRange sourceRange) {
            this.symbol = symbol;
            this.sourceRange = sourceRange;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof SymbolNotFoundError) {
                SymbolNotFoundError other = (SymbolNotFoundError) o;
                return Objects.equals(symbol, other.symbol)
                    && Objects.equals(sourceRange, other.sourceRange);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol, sourceRange);
        }

        @Override
        public String prettyPrint() {
            return "Symbol not found: " + symbol.quote() + " " + sourceRange.prettyPrint();
        }

        @Override
        public String toString() {
            return stringify(this) + "(symbol=" + symbol + ")";
        }
    }

    public static class TypeError extends SyntaxError {

        private final Unification unification;
        private final SourceRange sourceRange;

        private TypeError(Unification unification, SourceRange sourceRange) {
            this.unification = unification;
            this.sourceRange = sourceRange;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof TypeError) {
                TypeError other = (TypeError) o;
                return Objects.equals(unification, other.unification)
                    && Objects.equals(sourceRange, other.sourceRange);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(unification, sourceRange);
        }

        @Override
        public String prettyPrint() {
            return unification.prettyPrint() + " " + sourceRange.prettyPrint();
        }

        @Override
        public String toString() {
            return "TypeError(" + unification + ")";
        }
    }

    public static class TypeInstanceNotFoundError extends SyntaxError {

        private final TypeClassDescriptor typeClass;
        private final List<Type>          parameters;
        private final SourceRange         location;

        private TypeInstanceNotFoundError(TypeClassDescriptor typeClass, List<Type> parameters, SourceRange location) {
            this.typeClass = typeClass;
            this.parameters = ImmutableList.copyOf(parameters);
            this.location = location;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof TypeInstanceNotFoundError) {
                TypeInstanceNotFoundError other = (TypeInstanceNotFoundError) o;
                return Objects.equals(typeClass, other.typeClass)
                    && Objects.equals(parameters, other.parameters)
                    && Objects.equals(location, other.location);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(typeClass, parameters);
        }

        @Override
        public String prettyPrint() {
            return "Instance of type class " + quote(typeClass.getSymbol().getCanonicalName())
                + " not found for parameters [" + parameters.stream().map(Type::prettyPrint).collect(joining(", ")) + "]"
                + " " + location.prettyPrint();
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + typeClass + ", parameters=" + parameters + ")";
        }
    }
}
