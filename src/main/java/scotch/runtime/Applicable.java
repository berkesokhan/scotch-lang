package scotch.runtime;

@FunctionalInterface
public interface Applicable<A, B> extends Callable<Applicable<A, B>> {

    Callable<B> apply(Callable<A> argument);

    @Override
    default Applicable<A, B> call() {
        return this;
    }
}
