package scotch.data.eq;

import static java.util.Arrays.asList;
import static scotch.symbol.type.Types.sum;
import static scotch.runtime.RuntimeUtil.callable;

import java.util.List;
import scotch.symbol.InstanceGetter;
import scotch.symbol.TypeInstance;
import scotch.symbol.TypeParameters;
import scotch.symbol.type.Type;
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
