package scotch.data.eq;

import static java.util.Arrays.asList;
import static scotch.symbol.type.Types.sum;
import static scotch.runtime.RuntimeUtil.callable;

import java.util.List;
import scotch.symbol.InstanceGetter;
import scotch.symbol.type.Type;
import scotch.symbol.TypeInstance;
import scotch.symbol.TypeParameters;
import scotch.symbol.type.Types;
import scotch.runtime.Callable;

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
        return asList(Types.sum("scotch.data.int.Int"));
    }

    private EqInt() {
        // intentionally empty
    }

    @Override
    public Callable<Boolean> eq(Callable<Integer> left, Callable<Integer> right) {
        return callable(() -> left.call().equals(right.call()));
    }
}
