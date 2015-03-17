package scotch.data.list;

import static java.util.Arrays.asList;
import static scotch.symbol.type.Types.sum;
import static scotch.symbol.type.Types.var;
import static scotch.runtime.RuntimeUtil.callable;

import java.util.List;
import scotch.symbol.InstanceGetter;
import scotch.symbol.TypeInstance;
import scotch.symbol.TypeParameters;
import scotch.symbol.type.Type;
import scotch.data.eq.Eq;
import scotch.runtime.Callable;

@SuppressWarnings("unused")
@TypeInstance(typeClass = "scotch.data.eq.Eq")
public class EqList implements Eq<ConsList> {

    private static final Callable<EqList> INSTANCE = callable(EqList::new);

    @InstanceGetter
    public static Callable<EqList> instance() {
        return INSTANCE;
    }

    @TypeParameters
    public static List<Type> parameters() {
        return asList(sum("scotch.data.list.[]", var("a")));
    }

    private EqList() {
        // intentionally empty
    }

    @Override
    public Callable<Boolean> eq(Callable<ConsList> left, Callable<ConsList> right) {
        return callable(() -> left.call().equals(right.call()));
    }
}
