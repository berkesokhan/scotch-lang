package scotch.data.eq;

import static java.util.Arrays.asList;
import static scotch.symbol.type.Types.sum;
import static scotch.runtime.RuntimeSupport.callable;

import java.util.List;
import scotch.symbol.InstanceGetter;
import scotch.symbol.type.Type;
import scotch.symbol.TypeInstance;
import scotch.symbol.TypeParameters;
import scotch.symbol.type.Types;
import scotch.runtime.Callable;

@SuppressWarnings("unused")
@TypeInstance(typeClass = "scotch.data.eq.Eq")
public class EqBool implements Eq<Boolean> {

    private static final Callable<EqBool> INSTANCE = callable(EqBool::new);

    @InstanceGetter
    public static Callable<EqBool> instance() {
        return INSTANCE;
    }

    @TypeParameters
    public static List<Type> parameters() {
        return asList(Types.sum("scotch.data.int.Bool"));
    }

    private EqBool() {
        // intentionally empty
    }

    @Override
    public Callable<Boolean> eq(Callable<Boolean> left, Callable<Boolean> right) {
        return callable(() -> left.call().equals(right.call()));
    }
}
