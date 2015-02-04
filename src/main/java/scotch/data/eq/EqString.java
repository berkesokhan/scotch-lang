package scotch.data.eq;

import static java.util.Arrays.asList;
import static scotch.compiler.symbol.type.Types.sum;
import static scotch.runtime.RuntimeUtil.callable;

import java.util.List;
import scotch.compiler.symbol.InstanceGetter;
import scotch.compiler.symbol.TypeInstance;
import scotch.compiler.symbol.TypeParameters;
import scotch.compiler.symbol.type.Type;
import scotch.runtime.Callable;

@SuppressWarnings("unused")
@TypeInstance(typeClass = "scotch.data.eq.Eq")
public class EqString implements Eq<String> {

    private static final Callable<EqString> INSTANCE = callable(EqString::new);

    @InstanceGetter
    public static Callable<EqString> instance() {
        return INSTANCE;
    }

    @TypeParameters
    public static List<Type> parameters() {
        return asList(sum("scotch.data.string.String"));
    }

    private EqString() {
        // intentionally empty
    }

    @Override
    public Callable<Boolean> eq(Callable<String> left, Callable<String> right) {
        return callable(() -> left.call().equals(right.call()));
    }
}
