package scotch.runtime;

import java.util.function.Supplier;

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
