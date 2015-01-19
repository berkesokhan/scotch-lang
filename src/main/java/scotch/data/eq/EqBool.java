package scotch.data.eq;

import static java.util.Arrays.asList;
import static scotch.compiler.symbol.Type.sum;
import static scotch.runtime.RuntimeUtil.callable;

import java.util.List;
import scotch.compiler.symbol.InstanceGetter;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.TypeInstance;
import scotch.compiler.symbol.TypeParameters;
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
        return asList(sum("scotch.data.int.Bool"));
    }

    private EqBool() {
        // intentionally empty
    }

    @Override
    public Callable<Boolean> eq(Callable<Boolean> left, Callable<Boolean> right) {
        return callable(() -> left.call().equals(right.call()));
    }
}
