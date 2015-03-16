package scotch.compiler.syntax.value;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import scotch.compiler.syntax.reference.DefinitionReference;

public abstract class WithArguments {

    public static WithArguments withArguments(FunctionValue functionValue) {
        return new FunctionWithArguments(functionValue);
    }

    public static WithArguments withArguments(PatternMatcher patternMatcher) {
        return new PatternWithArguments(patternMatcher);
    }

    public static ValueWithoutArguments withoutArguments(Value value) {
        return new ValueWithoutArguments(value);
    }

    public abstract WithArguments map(BiFunction<DefinitionReference, List<Argument>, List<Argument>> function);

    public abstract Value orElseGet(Function<Value, Value> function);

    @AllArgsConstructor
    private static final class FunctionWithArguments extends WithArguments {

        private final FunctionValue functionValue;

        @Override
        public WithArguments map(BiFunction<DefinitionReference, List<Argument>, List<Argument>> function) {
            return withArguments(functionValue.withArguments(function.apply(functionValue.getReference(), functionValue.getArguments())));
        }

        @Override
        public Value orElseGet(Function<Value, Value> function) {
            return functionValue;
        }
    }

    @AllArgsConstructor
    private static final class PatternWithArguments extends WithArguments {

        private final PatternMatcher patternMatcher;

        @Override
        public WithArguments map(BiFunction<DefinitionReference, List<Argument>, List<Argument>> function) {
            return withArguments(patternMatcher.withArguments(function.apply(patternMatcher.getReference(), patternMatcher.getArguments())));
        }

        @Override
        public Value orElseGet(Function<Value, Value> function) {
            return patternMatcher;
        }
    }

    @AllArgsConstructor
    private static final class ValueWithoutArguments extends WithArguments {

        private final Value value;

        @Override
        public WithArguments map(BiFunction<DefinitionReference, List<Argument>, List<Argument>> function) {
            return this;
        }

        @Override
        public Value orElseGet(Function<Value, Value> function) {
            return function.apply(value);
        }
    }
}
