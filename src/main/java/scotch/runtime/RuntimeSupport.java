package scotch.runtime;

import java.util.function.Function;
import java.util.function.Supplier;

public final class RuntimeSupport {

    public static <A, B> Applicable<A, B> applicable(Function<Callable<A>, Callable<B>> function) {
        return function::apply;
    }

    public static Callable<Boolean> box(boolean value) { return new BoxedCallable<>(value); }

    public static Callable<Character> box(char value) { return new BoxedCallable<>(value); }

    public static Callable<Double> box(double value) {
        return new BoxedCallable<>(value);
    }

    public static Callable<Integer> box(int value) {
        return new BoxedCallable<>(value);
    }

    public static <A> Callable<A> box(A value) {
        return new BoxedCallable<>(value);
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

    @SuppressWarnings("unused")
    public static boolean unboxBool(Callable<Boolean> callable) {
        return callable.call();
    }

    private RuntimeSupport() {
        // intentionally empty
    }

    public static final class BoxedCallable<A> implements Callable<A> {

        private final A value;

        public BoxedCallable(A value) {
            this.value = value;
        }

        @SuppressWarnings("unchecked")
        @Override
        public A call() {
            if (value instanceof Callable) {
                return (A) ((Callable) value).call();
            } else {
                return value;
            }
        }
    }
}
