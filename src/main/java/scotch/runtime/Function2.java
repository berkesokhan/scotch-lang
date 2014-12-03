package scotch.runtime;

public interface Function2<A, B, C> {

    C apply(Callable<A> a, Callable<B> b);
}
