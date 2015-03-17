package scotch.runtime;

import java.util.function.Supplier;

/**
 * A thunk which uses a supplier to evaluate its value.
 *
 * @param <A> The type contained by this {@link Thunk}.
 */
public class SuppliedThunk<A> extends Thunk<A> {

    private final Supplier<A> supplier;

    public SuppliedThunk(Supplier<A> supplier) {
        this.supplier = supplier;
    }

    @Override
    protected A evaluate() {
        return supplier.get();
    }
}
