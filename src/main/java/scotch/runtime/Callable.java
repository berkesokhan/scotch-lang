package scotch.runtime;

/**
 * A boxed type which contains a suspended execution or evaluation.
 *
 * @param <A> The type contained within the {@link Callable}.
 */
@FunctionalInterface
public interface Callable<A> {

    /**
     * Evaluates and returns the value contained within the {@link Callable}.
     *
     * @return The evaluated value.
     */
    A call();
}
