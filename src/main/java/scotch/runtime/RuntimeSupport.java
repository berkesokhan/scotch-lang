package scotch.runtime;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Support methods and classes.
 */
public final class RuntimeSupport {

    /**
     * Shorthand for creating an {@link Applicable}.
     *
     * @param function The function to alias into an {@link Applicable}.
     * @param <A> The type of the {@link Callable} argument.
     * @param <B> The type of the {@link Callable} result.
     * @return The appropriate {@link Applicable}.
     */
    public static <A, B> Applicable<A, B> applicable(Function<Callable<A>, Callable<B>> function) {
        return function::apply;
    }

    /**
     * Boxes a boolean into a {@link Callable}.
     *
     * @param value The boolean to be boxed.
     * @return The boxed boolean.
     */
    public static Callable<Boolean> box(boolean value) { return new BoxedCallable<>(value); }

    /**
     * Boxes a char into a {@link Callable}.
     *
     * @param value The char to be boxed.
     * @return The boxed char.
     */
    public static Callable<Character> box(char value) { return new BoxedCallable<>(value); }

    /**
     * Boxes a double into a {@link Callable}.
     *
     * @param value The double to be boxed.
     * @return The boxed double.
     */
    public static Callable<Double> box(double value) {
        return new BoxedCallable<>(value);
    }

    /**
     * Boxes an int into a {@link Callable}.
     *
     * @param value The int to be boxed.
     * @return The boxed int.
     */
    public static Callable<Integer> box(int value) {
        return new BoxedCallable<>(value);
    }

    /**
     * Boxes any Object value into a {@link Callable}.
     *
     * @param value The value to be boxed.
     * @param <A> The type of the value.
     * @return The boxed value.
     */
    public static <A> Callable<A> box(A value) {
        return new BoxedCallable<>(value);
    }

    /**
     * Creates a {@link Thunk} which will execute the given Supplier and store
     * the resultant value when called.
     *
     * @param supplier The supplier giving the value.
     * @param <A> The type returned from the supplier.
     * @return The thunk.
     */
    public static <A> Callable<A> callable(Supplier<A> supplier) {
        return new Thunk<A>() {
            @Override
            protected A evaluate() {
                return supplier.get();
            }
        };
    }

    /**
     * Creates a {@link Thunk} that handles a Supplier returning a {@link Callable}.
     *
     * @param supplier The supplier returning a {@link Callable}
     * @param <A> The type of the value returned from the {@link Callable}
     * @return The thunk.
     */
    public static <A> Callable<A> flatCallable(Supplier<Callable<A>> supplier) {
        return new Thunk<A>() {
            @Override
            protected A evaluate() {
                return supplier.get().call();
            }
        };
    }

    /**
     * Unboxes a boolean from a {@link Callable}.
     *
     * @param callable The callable to unbox.
     * @return The boolean value.
     */
    @SuppressWarnings("unused")
    public static boolean unboxBool(Callable<Boolean> callable) {
        return callable.call();
    }

    private RuntimeSupport() {
        // intentionally empty
    }

    /**
     * Boxes values into {@link Callable}s.
     *
     * @param <A> The contained type.
     */
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
