package scotch.runtime;

@FunctionalInterface
public interface Callable<A> {

    static Callable<Integer> box(int value) {
        return new BoxedCallable<>(value);
    }

    static Callable<Double> box(double value) {
        return new BoxedCallable<>(value);
    }

    static <A> Callable<A> box(A value) {
        return new BoxedCallable<>(value);
    }

    A call();

    static final class BoxedCallable<A> implements Callable<A> {

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
