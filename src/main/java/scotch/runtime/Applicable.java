package scotch.runtime;

/**
 * A lambda type which takes an argument and returns a {@link Callable} suspending the execution.
 *
 * @param <A> The type of the {@link Applicable}'s argument.
 * @param <B> The type of the {@link Applicable}'s result.
 */
@FunctionalInterface
public interface Applicable<A, B> extends Callable<Applicable<A, B>> {

    /**
     * Applies a {@link Callable} argument to this lambda, returning a {@link Callable} thunk.
     *
     * @param argument The argument to be applied.
     * @return A thunk.
     */
    Callable<B> apply(Callable<A> argument);

    /**
     * Evaluates this {@link Applicable}, simply returning itself as it is already head-normal form.
     *
     * @return This {@link Applicable}.
     */
    @Override
    default Applicable<A, B> call() {
        return this;
    }
}
