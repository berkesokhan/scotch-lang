package scotch.compiler.symbol;

import static java.lang.String.join;
import static java.util.stream.Collectors.toList;
import static scotch.util.StringUtil.stringify;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import com.google.common.collect.ImmutableSet;
import scotch.compiler.symbol.Type.VariableType;

public abstract class Unification {

    public static Unification circular(Type expected, Type reference) {
        return new CircularReference(expected, reference);
    }

    public static Unification contextMismatch(Type expected, Type actual, Collection<Symbol> expectedContext, Collection<Symbol> actualContext) {
        return new ContextMismatch(expected, actual, expectedContext, actualContext);
    }

    public static Unification failedBinding(Type targetType, VariableType variableType, Type variableTarget) {
        return new FailedBinding(targetType, variableType, variableTarget);
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

    @Override
    public abstract boolean equals(Object o);

    public abstract Unification flip();

    @Override
    public abstract int hashCode();

    public abstract boolean isUnified();

    public Unification map(Function<? super Type, ? extends Unification> function) {
        return this;
    }

    public Type orElseGet(Function<Unification, Type> function) {
        return function.apply(this);
    }

    public abstract String prettyPrint();

    @Override
    public abstract String toString();

    public interface UnificationVisitor<T> {

        default T visit(CircularReference circularReference) {
            return visitOtherwise(circularReference);
        }

        default T visit(ContextMismatch contextMismatch) {
            return visitOtherwise(contextMismatch);
        }

        default T visit(FailedBinding failedBinding) {
            return visitOtherwise(failedBinding);
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
        public Unification flip() {
            return this;
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
            return "Circular type reference: type " + reference
                + " is referenced by target type " + expected;
        }

        @Override
        public String toString() {
            return stringify(this) + "(expected=" + expected + ", reference=" + reference + ")";
        }
    }

    public static class ContextMismatch extends Unification {

        private final Type        expected;
        private final Type        actual;
        private final Set<Symbol> expectedContext;
        private final Set<Symbol> actualContext;

        public ContextMismatch(Type expected, Type actual, Collection<Symbol> expectedContext, Collection<Symbol> actualContext) {
            this.expected = expected;
            this.actual = actual;
            this.actualContext = ImmutableSet.copyOf(actualContext);
            this.expectedContext = ImmutableSet.copyOf(expectedContext);
        }

        @Override
        public <T> T accept(UnificationVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof ContextMismatch) {
                ContextMismatch other = (ContextMismatch) o;
                return Objects.equals(expected, other.expected)
                    && Objects.equals(actual, other.actual)
                    && Objects.equals(expectedContext, other.expectedContext)
                    && Objects.equals(actualContext, other.actualContext);
            } else {
                return false;
            }
        }

        @Override
        public Unification flip() {
            return new ContextMismatch(actual, expected, expectedContext, actualContext);
        }

        @Override
        public int hashCode() {
            return Objects.hash(expected, actual, expectedContext, actualContext);
        }

        @Override
        public boolean isUnified() {
            return false;
        }

        @Override
        public String prettyPrint() {
            Set<Symbol> contextDifference = new HashSet<>();
            contextDifference.addAll(expectedContext);
            contextDifference.removeAll(actualContext);
            return "Type mismatch: " + actual
                + " does not implement entire context of " + expected + ":"
                + " difference is [" + join(", ", contextDifference.stream().map(Symbol::getCanonicalName).collect(toList())) + "]";
        }

        @Override
        public String toString() {
            return stringify(this) + "(expected=" + expected + ", actual=" + actual + ", expectedContext=" + expectedContext + ", actualContext=" + actualContext + ")";
        }
    }

    public static class FailedBinding extends Unification {

        private final Type         targetType;
        private final VariableType variableType;
        private final Type         variableTarget;

        public FailedBinding(Type targetType, VariableType variableType, Type variableTarget) {
            this.targetType = targetType;
            this.variableType = variableType;
            this.variableTarget = variableTarget;
        }

        @Override
        public <T> T accept(UnificationVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof FailedBinding) {
                FailedBinding other = (FailedBinding) o;
                return Objects.equals(targetType, other.targetType)
                    && Objects.equals(variableType, other.variableType)
                    && Objects.equals(variableTarget, other.variableTarget);
            } else {
                return false;
            }
        }

        @Override
        public Unification flip() {
            return this;
        }

        @Override
        public int hashCode() {
            return Objects.hash(targetType, variableType, variableTarget);
        }

        @Override
        public boolean isUnified() {
            return false;
        }

        @Override
        public String prettyPrint() {
            return "Can't re-bind type " + variableType + " to new target " + targetType
                + "; current binding is incompatible: " + variableTarget;
        }

        @Override
        public String toString() {
            return stringify(this) + "(targetType=" + targetType + ", variableType=" + variableType + ", variableTarget=" + variableTarget + ")";
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
        public Unification flip() {
            return new TypeMismatch(actual, expected);
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
            return "Type mismatch: expected type " + expected + " but got " + actual;
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
        public boolean equals(Object o) {
            return o == this || o instanceof Unified && Objects.equals(unifiedType, ((Unified) o).unifiedType);
        }

        @Override
        public Unification flip() {
            return this;
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
        public Unification map(Function<? super Type, ? extends Unification> function) {
            return function.apply(unifiedType);
        }

        @Override
        public Type orElseGet(Function<Unification, Type> function) {
            return unifiedType;
        }

        @Override
        public String prettyPrint() {
            return "Successful unification to target type: " + unifiedType;
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + unifiedType + ")";
        }
    }
}
