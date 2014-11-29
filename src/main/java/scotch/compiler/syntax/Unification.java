package scotch.compiler.syntax;

import static java.lang.String.join;
import static java.util.stream.Collectors.toList;
import static scotch.compiler.util.TextUtil.stringify;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import com.google.common.collect.ImmutableSet;

public abstract class Unification {

    public static Unification circular(Type expected, Type reference) {
        return new CircularReference(expected, reference);
    }

    public static Unification contextMismatch(Type expected, Type actual, Collection<Symbol> contextDifference) {
        return new ContextMismatch(expected, actual, contextDifference);
    }

    public static Unification mismatch(Type expected, Type actual) {
        return new TypeMismatch(expected, actual);
    }

    public static Unification unified(Type result) {
        return new Unified(result);
    }

    private Unification() {
        // intentionally empty
    }

    public abstract <T> T accept(UnificationVisitor<T> visitor);

    public abstract Unification andThen(Binding binding);

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    public abstract boolean isUnified();

    public abstract String prettyPrint();

    @Override
    public abstract String toString();

    @FunctionalInterface
    public interface Binding {

        Unification apply(Type result);
    }

    public interface UnificationVisitor<T> {

        default T visit(CircularReference circularReference) {
            return visitOtherwise(circularReference);
        }

        default T visit(ContextMismatch contextMismatch) {
            return visitOtherwise(contextMismatch);
        }

        default T visit(TypeMismatch typeMismatch) {
            return visitOtherwise(typeMismatch);
        }

        default T visit(Unified unified) {
            return visitOtherwise(unified);
        }

        default T visitOtherwise(Unification unification) {
            throw new UnsupportedOperationException("Can't visit " + unification.getClass().getSimpleName());
        }
    }

    public static class CircularReference extends Unification {

        private final Type expected;
        private final Type reference;

        private CircularReference(Type expected, Type reference) {
            this.expected = expected;
            this.reference = reference;
        }

        @Override
        public <T> T accept(UnificationVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public Unification andThen(Binding binding) {
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof CircularReference) {
                CircularReference other = (CircularReference) o;
                return Objects.equals(expected, other.expected)
                    && Objects.equals(reference, other.reference);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(expected, reference);
        }

        @Override
        public boolean isUnified() {
            return false;
        }

        @Override
        public String prettyPrint() {
            return "Circular type reference: type " + reference.prettyPrint()
                + " is referenced by target type " + expected.prettyPrint();
        }

        @Override
        public String toString() {
            return stringify(this) + "(expected=" + expected + ", reference=" + reference + ")";
        }
    }

    public static class ContextMismatch extends Unification {

        private final Type        expected;
        private final Type        actual;
        private final Set<Symbol> contextDifference;

        public ContextMismatch(Type expected, Type actual, Collection<Symbol> contextDifference) {
            this.expected = expected;
            this.actual = actual;
            this.contextDifference = ImmutableSet.copyOf(contextDifference);
        }

        @Override
        public <T> T accept(UnificationVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public Unification andThen(Binding binding) {
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof ContextMismatch) {
                ContextMismatch other = (ContextMismatch) o;
                return Objects.equals(expected, other.expected)
                    && Objects.equals(actual, other.actual)
                    && Objects.equals(contextDifference, other.contextDifference);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(expected, actual, contextDifference);
        }

        @Override
        public boolean isUnified() {
            return false;
        }

        @Override
        public String prettyPrint() {
            return "Type mismatch: " + actual.prettyPrint()
                + " does not implement entire context of " + expected.prettyPrint() + ":"
                + " difference is [" + join(", ", contextDifference.stream().map(Symbol::getCanonicalName).collect(toList())) + "]";
        }

        @Override
        public String toString() {
            return stringify(this) + "(expected=" + expected + ", actual=" + actual + ", contextDifference=" + contextDifference + ")";
        }
    }

    public static class TypeMismatch extends Unification {

        private final Type expected;
        private final Type actual;

        private TypeMismatch(Type expected, Type actual) {
            this.expected = expected;
            this.actual = actual;
        }

        @Override
        public <T> T accept(UnificationVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public Unification andThen(Binding binding) {
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof TypeMismatch) {
                TypeMismatch other = (TypeMismatch) o;
                return Objects.equals(expected, other.expected)
                    && Objects.equals(actual, other.actual);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(expected, actual);
        }

        @Override
        public boolean isUnified() {
            return false;
        }

        @Override
        public String prettyPrint() {
            return "Type mismatch: expected type " + expected.prettyPrint() + " but got " + actual.prettyPrint();
        }

        @Override
        public String toString() {
            return stringify(this) + "(expected=" + expected + ", actual=" + actual + ")";
        }
    }

    public static class Unified extends Unification {

        private final Type unifiedType;

        private Unified(Type unifiedType) {
            this.unifiedType = unifiedType;
        }

        @Override
        public <T> T accept(UnificationVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public Unification andThen(Binding binding) {
            return binding.apply(unifiedType);
        }

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof Unified && Objects.equals(unifiedType, ((Unified) o).unifiedType);
        }

        public Type getUnifiedType() {
            return unifiedType;
        }

        @Override
        public int hashCode() {
            return Objects.hash(unifiedType);
        }

        @Override
        public boolean isUnified() {
            return true;
        }

        @Override
        public String prettyPrint() {
            return "Successful unification to target type: " + unifiedType.prettyPrint();
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + unifiedType + ")";
        }
    }
}
