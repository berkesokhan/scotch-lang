package scotch.runtime;

@FunctionalInterface
public interface Callable<A> {

    static Callable<Integer> box(int value) {
        return new IntCallable(value);
    }

    A call();

    static final class IntCallable implements Callable<Integer> {

        private final Integer value;

        public IntCallable(int value) {
            this.value = value;
        }

        @Override
        public Integer call() {
            return value;
        }
    }
}
