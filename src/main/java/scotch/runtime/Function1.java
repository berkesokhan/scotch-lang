package scotch.runtime;

public interface Function1<A, B> {

    B apply(Callable<A> a);
}
