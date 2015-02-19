package scotch.compiler.symbol.type;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

public abstract class TailApplication {

    public static TailApplication left(Unification unification) {
        return new LeftApplication(unification);
    }

    public static TailApplication right(List<Type> unifiedParameters, List<Type> remainingParameters) {
        return new RightApplication(unifiedParameters, remainingParameters);
    }

    private TailApplication() {
        // intentionally empty
    }

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    public abstract TailApplication next(Function<Type, Unification> function);

    @Override
    public abstract String toString();

    public abstract Unification unify(BiFunction<List<Type>, List<Type>, Unification> function);

    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    @ToString
    private static class LeftApplication extends TailApplication {

        private final Unification unification;

        @Override
        public TailApplication next(Function<Type, Unification> function) {
            return this;
        }

        @Override
        public Unification unify(BiFunction<List<Type>, List<Type>, Unification> function) {
            return unification;
        }
    }

    @AllArgsConstructor
    @EqualsAndHashCode(callSuper = false)
    @ToString
    private static class RightApplication extends TailApplication {

        private final List<Type> unifiedParameters;
        private final List<Type> remainingParameters;

        @Override
        public TailApplication next(Function<Type, Unification> function) {
            Unification unifiedParameter = function.apply(remainingParameters.get(0));
            if (unifiedParameter.isUnified()) {
                unifiedParameter.ifUnified(unifiedParameters::add);
                remainingParameters.remove(0);
                return this;
            } else {
                return left(unifiedParameter);
            }
        }

        @Override
        public Unification unify(BiFunction<List<Type>, List<Type>, Unification> function) {
            return function.apply(unifiedParameters, remainingParameters);
        }
    }
}
