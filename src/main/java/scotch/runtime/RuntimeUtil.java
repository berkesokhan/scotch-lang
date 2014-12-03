package scotch.runtime;

import java.util.function.Function;
import java.util.function.Supplier;

public final class RuntimeUtil {

    public static <A, B> Applicable<A, B> applicable(Function<Callable<A>, ? extends Callable<B>> function) {
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

    public static <A, B> Applicable<A, B> fn1(Function1<A, B> function) {
        return applicable(a -> callable(() -> function.apply(a)));
    }

    public static <A, B, C> Applicable<A, Applicable<B, C>> fn2(Function2<A, B, C> function) {
        return applicable(a -> applicable(b -> callable(() -> function.apply(a, b))));
    }

    private RuntimeUtil() {
        // intentionally empty
    }
}
