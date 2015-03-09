package scotch.data.list;

import static java.util.Arrays.asList;
import static scotch.compiler.symbol.type.Types.sum;
import static scotch.compiler.symbol.type.Types.var;
import static scotch.runtime.RuntimeUtil.callable;

import java.util.List;
import scotch.compiler.symbol.InstanceGetter;
import scotch.compiler.symbol.TypeInstance;
import scotch.compiler.symbol.TypeParameters;
import scotch.compiler.symbol.type.Type;
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
