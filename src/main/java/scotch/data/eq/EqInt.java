package scotch.data.eq;

import static java.util.Arrays.asList;
import static scotch.runtime.RuntimeSupport.callable;

import java.util.List;
import scotch.data.int_.Int;
import scotch.runtime.Callable;
import scotch.symbol.InstanceGetter;
import scotch.symbol.TypeInstance;
import scotch.symbol.TypeParameters;
import scotch.symbol.type.Type;

@SuppressWarnings("unused")
@TypeInstance(typeClass = "scotch.data.eq.Eq")
public class EqInt implements Eq<Integer> {

    private static final Callable<EqInt> INSTANCE = callable(EqInt::new);

    @InstanceGetter
    public static Callable<EqInt> instance() {
        return INSTANCE;
    }

    @TypeParameters
    public static List<Type> parameters() {
        return asList(Int.TYPE);
    }

    private EqInt() {
        // intentionally empty
    }

    @Override
    public Callable<Boolean> eq(Callable<Integer> left, Callable<Integer> right) {
        return callable(() -> left.call().equals(right.call()));
    }
}
