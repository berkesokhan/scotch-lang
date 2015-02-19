package scotch.compiler.symbol.type;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import scotch.compiler.util.Pair;

public abstract class HeadZip {

    public static HeadZip left() {
        return new LeftZip();
    }

    public static HeadZip right(Pair<Type, Type> zippedPair, List<Type> remainingParameters) {
        return new RightZip(zippedPair, remainingParameters);
    }

    private HeadZip() {
        // intentionally empty
    }

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();

    public abstract Optional<List<Pair<Type, Type>>> zip(BiFunction<Pair<Type, Type>, List<Type>, Optional<List<Pair<Type, Type>>>> function);

    @EqualsAndHashCode(callSuper = false)
    @ToString
    private static final class LeftZip extends HeadZip {

        @Override
        public Optional<List<Pair<Type, Type>>> zip(BiFunction<Pair<Type, Type>, List<Type>, Optional<List<Pair<Type, Type>>>> function) {
            return Optional.empty();
        }
    }

    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    @ToString
    private static final class RightZip extends HeadZip {

        private final Pair<Type, Type> zippedPair;
        private final List<Type>       remainingParameters;

        public Optional<List<Pair<Type, Type>>> zip(BiFunction<Pair<Type, Type>, List<Type>, Optional<List<Pair<Type, Type>>>> function) {
            return function.apply(zippedPair, remainingParameters);
        }
    }
}
