package scotch.compiler.symbol.type;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import scotch.compiler.util.Pair;

public abstract class TailZip {

    public static TailZip right(List<Pair<Type, Type>> zippedPairs, List<Type> remainingParameters) {
        return new RightZip(zippedPairs, remainingParameters);
    }

    public static TailZip left() {
        return new LeftZip();
    }

    private TailZip() {
        // intentionally empty
    }

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    public abstract TailZip next(Function<Type, Optional<List<Pair<Type, Type>>>> function);

    @Override
    public abstract String toString();

    public abstract Optional<List<Pair<Type,Type>>> zip(BiFunction<List<Pair<Type,Type>>, List<Type>, Optional<List<Pair<Type,Type>>>> function);

    @EqualsAndHashCode(callSuper = false)
    @ToString
    private static final class LeftZip extends TailZip {

        @Override
        public TailZip next(Function<Type, Optional<List<Pair<Type, Type>>>> function) {
            return this;
        }

        @Override
        public Optional<List<Pair<Type, Type>>> zip(BiFunction<List<Pair<Type, Type>>, List<Type>, Optional<List<Pair<Type, Type>>>> function) {
            return Optional.empty();
        }
    }

    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    @ToString
    private static final class RightZip extends TailZip {

        private final List<Pair<Type, Type>> zippedPairs;
        private final List<Type> remainingParameters;

        @Override
        public TailZip next(Function<Type, Optional<List<Pair<Type, Type>>>> function) {
            return function.apply(remainingParameters.get(0))
                .map(zippedPairs::addAll)
                .map(b -> remainingParameters.remove(0))
                .map(b -> right(zippedPairs, remainingParameters))
                .orElseGet(TailZip::left);
        }

        @Override
        public Optional<List<Pair<Type, Type>>> zip(BiFunction<List<Pair<Type, Type>>, List<Type>, Optional<List<Pair<Type, Type>>>> function) {
            return function.apply(zippedPairs, remainingParameters);
        }
    }
}
