package scotch.runtime;

public abstract class Thunk<A> implements Callable<A> {

    private volatile A value;

    @SuppressWarnings("unchecked")
    @Override
    public A call() {
        if (value == null) {
            synchronized (this) {
                if (value == null) {
                    value = evaluate();
                    while (value instanceof Callable) {
                        if (value instanceof Applicable) {
                            break;
                        }
                        value = ((Callable<A>) value).call();
                    }
                }
            }
        }
        return value;
    }

    protected abstract A evaluate();
}
