package scotch.data.ord;

import static java.util.Arrays.asList;
import static scotch.runtime.RuntimeSupport.callable;

import java.util.List;
import scotch.data.eq.Eq;
import scotch.runtime.Callable;
import scotch.symbol.InstanceGetter;
import scotch.symbol.TypeInstance;
import scotch.symbol.TypeParameters;
import scotch.symbol.type.Type;

@SuppressWarnings("unused")
@TypeInstance(typeClass = "scotch.data.eq.Eq")
public class EqOrdering implements Eq<Ordering> {

    private static final Callable<EqOrdering> INSTANCE = callable(EqOrdering::new);

    @InstanceGetter
    public static Callable<EqOrdering> instance() {
        return INSTANCE;
    }

    @TypeParameters
    public static List<Type> parameters() {
        return asList(Ordering.TYPE);
    }

    @Override
    public Callable<Boolean> eq(Callable<Ordering> left, Callable<Ordering> right) {
        return callable(() -> left.call() == right.call());
    }
}
