package scotch.runtime;

import java.util.function.Function;
import java.util.function.Supplier;

public final class RuntimeUtil {

    public static <A, B> Applicable<A, B> applicable(Function<Callable<A>, Callable<B>> function) {
        return function::apply;
    }

    public static <A> Callable<A> callable(Supplier<A> supplier) {
        return new Thunk<A>() {
            @Override
            protected A evaluate() {
                return supplier.get();
            }
        };
    }

    public static <A> Callable<A> flatCallable(Supplier<Callable<A>> supplier) {
        return new Thunk<A>() {
            @Override
            protected A evaluate() {
                return supplier.get().call();
            }
        };
    }

    private RuntimeUtil() {
        // intentionally empty
    }
}
