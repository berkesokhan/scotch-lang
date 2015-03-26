package scotch.symbol.type;

import java.util.List;
import java.util.function.BiFunction;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

public abstract class HeadApplication {

    public static HeadApplication left(Unification unification) {
        return new LeftApplication(unification);
    }

    public static HeadApplication right(SumType type, List<Type> remainingParameters) {
        return new RightApplication(type, remainingParameters);
    }

    private HeadApplication() {
        // intentionally empty
    }

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();

    public abstract Unification unify(BiFunction<SumType, List<Type>, Unification> function);

    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    @ToString
    private static class LeftApplication extends HeadApplication {

        private final Unification unification;

        @Override
        public Unification unify(BiFunction<SumType, List<Type>, Unification> function) {
            return unification;
        }
    }

    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    @ToString
    private static class RightApplication extends HeadApplication {

        private final SumType    type;
        private final List<Type> remainingParameters;

        @Override
        public Unification unify(BiFunction<SumType, List<Type>, Unification> function) {
            return function.apply(type, remainingParameters);
        }
    }
}
