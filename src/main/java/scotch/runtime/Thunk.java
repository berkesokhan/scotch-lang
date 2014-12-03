package scotch.runtime;

import java.util.concurrent.locks.ReentrantLock;

public abstract class Thunk<A> implements Callable<A> {

    private final ReentrantLock lock = new ReentrantLock();
    private volatile A value;

    @SuppressWarnings("unchecked")
    @Override
    public A call() {
        if (value == null) {
            lock.lock();
            try {
                if (value == null) {
                    value = evaluate();
                    while (value instanceof Callable) {
                        if (value instanceof Applicable) {
                            break;
                        }
                        value = ((Callable<A>) value).call();
                    }
                }
            } finally {
                lock.unlock();
            }
        }
        return value;
    }

    protected abstract A evaluate();
}
