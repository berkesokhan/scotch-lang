package scotch.runtime;

@FunctionalInterface
public interface Callable<A> {

    A call();
}
