package scotch.symbol.type;

import static java.lang.String.join;
import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import com.google.common.collect.ImmutableSet;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import scotch.symbol.Symbol;

@AllArgsConstructor(access = PRIVATE)
@EqualsAndHashCode(callSuper = false)
@ToString
public abstract class Unification {

    public static Unification circular(Type expected, Type reference) {
        return new CircularReference(expected, reference);
    }

    public static Unification contextMismatch(Type expected, Type actual, Collection<Symbol> expectedContext, Collection<Symbol> actualContext) {
        return new ContextMismatch(expected, actual, expectedContext, actualContext);
    }

    public static Unification extraParameter(Type parameter) {
        return new ExtraParameter(parameter);
    }

    public static Unification failedBinding(Type targetType, VariableType variableType, Type variableTarget) {
        return new FailedBinding(targetType, variableType, variableTarget);
    }

    public static Unification mismatch(Type expected, Type actual) {
        return new TypeMismatch(expected, actual);
    }

    public static Unification mismatch(Type expected, Type actual, Unification cause) {
        return new TypeMismatch(expected, actual, cause);
    }

    public static Unification missingParameter(Type parameter) {
        return new MissingParameter(parameter);
    }

    public static Unification unified(Type result) {
        return new Unified(result);
    }

    protected final Optional<Unification> cause;

    private Unification() {
        cause = Optional.empty();
    }

    public Unification flip() {
        return this;
    }

    public void ifUnified(Consumer<Type> consumer) {
        // intentionally empty
    }

    public boolean isUnified() {
        return false;
    }

    @SuppressWarnings("unchecked")
    public Unification map(Function<? super Type, ? extends Unification> function) {
        return this;
    }

    public Unification mapType(Function<Type, Type> function) {
        return this;
    }

    public Type orElseGet(Function<Unification, Type> function) {
        return function.apply(this);
    }

    public Unification orElseMap(Function<Unification, Unification> function) {
        return function.apply(this);
    }

    public <T extends Throwable> Type orElseThrow(Function<Unification, T> function) throws T {
        throw function.apply(this);
    }

    public abstract String prettyPrint();

    @AllArgsConstructor(access = PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @ToString
    public static class CircularReference extends Unification {

        private final Type expected;
        private final Type reference;

        @Override
        public String prettyPrint() {
            return "Circular type reference: type " + reference
                + " is referenced by target type " + expected;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    @ToString
    public static class ContextMismatch extends Unification {

        private final Type        expected;
        private final Type        actual;
        private final Set<Symbol> expectedContext;
        private final Set<Symbol> actualContext;

        private ContextMismatch(Type expected, Type actual, Collection<Symbol> expectedContext, Collection<Symbol> actualContext) {
            this.expected = expected;
            this.actual = actual;
            this.actualContext = ImmutableSet.copyOf(actualContext);
            this.expectedContext = ImmutableSet.copyOf(expectedContext);
        }

        @Override
        public Unification flip() {
            return new ContextMismatch(actual, expected, expectedContext, actualContext);
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
    }

    @AllArgsConstructor(access = PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @ToString
    public static class ExtraParameter extends Unification {

        private final Type parameter;

        @Override
        public String prettyPrint() {
            return "Extra parameter " + parameter;
        }
    }

    @AllArgsConstructor(access = PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @ToString
    public static class FailedBinding extends Unification {

        private final Type         targetType;
        private final VariableType variableType;
        private final Type         variableTarget;

        @Override
        public String prettyPrint() {
            return "Can't re-bind type " + variableType + " to new target " + targetType
                + "; current binding is incompatible: " + variableTarget;
        }
    }

    @AllArgsConstructor(access = PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @ToString
    public static class MissingParameter extends Unification {

        private final Type parameter;

        @Override
        public String prettyPrint() {
            return "Missing parameter " + parameter;
        }
    }

    @AllArgsConstructor(access = PRIVATE)
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class TypeMismatch extends Unification {

        private final Type expected;
        private final Type actual;

        private TypeMismatch(Type expected, Type actual, Unification cause) {
            super(Optional.of(cause));
            this.expected = expected;
            this.actual = actual;
        }

        @Override
        public Unification flip() {
            return new TypeMismatch(actual, expected);
        }

        @Override
        public String prettyPrint() {
            return "Type mismatch: expected type " + expected + " but got " + actual
                + cause.map(c -> "\nCaused by: " + c.prettyPrint()).orElse("");
        }
    }

    @AllArgsConstructor(access = PRIVATE)
    @EqualsAndHashCode(callSuper = false)
    @ToString
    public static class Unified extends Unification {

        private final Type unifiedType;

        @Override
        public void ifUnified(Consumer<Type> consumer) {
            consumer.accept(unifiedType);
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
        public Unification mapType(Function<Type, Type> function) {
            return unified(function.apply(unifiedType));
        }

        @Override
        public Type orElseGet(Function<Unification, Type> function) {
            return unifiedType;
        }

        @Override
        public Unification orElseMap(Function<Unification, Unification> function) {
            return this;
        }

        @Override
        public <T extends Throwable> Type orElseThrow(Function<Unification, T> function) throws T {
            return unifiedType;
        }

        @Override
        public String prettyPrint() {
            return "Successful unification to target type: " + unifiedType;
        }
    }
}
