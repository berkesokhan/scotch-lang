package scotch.lang;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.util.TextUtil.stringify;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class Unification {

    public static Unification circular(Type expected, Type reference) {
        return new CircularReference(expected, reference);
    }

    public static Unification mismatch(Type expected, Type actual) {
        return new TypeMismatch(expected, actual);
    }

    public static Unification mismatch(Type expected, Type actual, TypeScope typeScope) {
        return new ContextMismatch(expected, typeScope.getContext(expected), actual, typeScope.getContext(actual));
    }

    public static Unification unified(Type result) {
        return new Unified(result);
    }

    private Unification() {
        // intentionally empty
    }

    public abstract Unification andThen(Binding binding);

    @Override
    public abstract boolean equals(Object o);

    public abstract Type getUnifiedType();

    @Override
    public abstract int hashCode();

    public abstract boolean isUnified();

    @Override
    public abstract String toString();

    @FunctionalInterface
    public static interface Binding {

        Unification apply(Type result);
    }

    public static class CircularReference extends Unification {

        private final Type expected;
        private final Type reference;

        private CircularReference(Type expected, Type reference) {
            this.expected = expected;
            this.reference = reference;
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
        public Type getUnifiedType() {
            throw new IllegalStateException();
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
        public String toString() {
            return stringify(this) + "(expected=" + expected + ", reference=" + reference + ")";
        }
    }

    public static class ContextMismatch extends Unification {

        private final Type         expectedType;
        private final List<String> expectedContext;
        private final Type         actualType;
        private final List<String> actualContext;

        public ContextMismatch(Type expectedType, List<String> expectedContext, Type actualType, List<String> actualContext) {
            this.expectedType = expectedType;
            this.expectedContext = expectedContext;
            this.actualType = actualType;
            this.actualContext = actualContext;
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
                return Objects.equals(expectedType, other.expectedType)
                    && Objects.equals(expectedContext, other.expectedContext)
                    && Objects.equals(actualType, other.actualType)
                    && Objects.equals(actualContext, other.actualContext);
            } else {
                return false;
            }
        }

        @Override
        public Type getUnifiedType() {
            throw new IllegalStateException();
        }

        @Override
        public int hashCode() {
            return Objects.hash(expectedType, expectedContext, actualType, actualContext);
        }

        @Override
        public boolean isUnified() {
            return false;
        }

        @Override
        public String toString() {
            List<String> difference = new ArrayList<>();
            difference.addAll(expectedContext.stream().filter(context -> !actualContext.contains(context)).collect(toList()));
            difference.addAll(actualContext.stream().filter(context -> !expectedContext.contains(context)).collect(toList()));
            return stringify(this) + "(expected=" + expectedType + ", actual=" + actualType + ", difference=" + difference + ")";
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
        public Type getUnifiedType() {
            throw new IllegalStateException();
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
        public Unification andThen(Binding binding) {
            return binding.apply(unifiedType);
        }

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof Unified && Objects.equals(unifiedType, ((Unified) o).unifiedType);
        }

        @Override
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
        public String toString() {
            return stringify(this) + "(" + unifiedType + ")";
        }
    }
}
