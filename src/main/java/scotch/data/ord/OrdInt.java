package scotch.data.ord;

import static java.util.Arrays.asList;
import static scotch.runtime.RuntimeSupport.callable;
import static scotch.symbol.type.Types.sum;

import java.util.List;
import scotch.data.eq.Eq;
import scotch.runtime.Callable;
import scotch.symbol.InstanceGetter;
import scotch.symbol.TypeInstance;
import scotch.symbol.TypeParameters;
import scotch.symbol.type.Type;

@SuppressWarnings("unused")
@TypeInstance(typeClass = "scotch.data.ord.Ord")
public class OrdInt implements Ord<Integer> {

    private static final Callable<OrdInt> INSTANCE = callable(OrdInt::new);

    @InstanceGetter
    public static Callable<OrdInt> instance() {
        return INSTANCE;
    }

    @TypeParameters
    public static List<Type> parameters() {
        return asList(sum("scotch.data.int.Int"));
    }

    @Override
    public Callable<Boolean> lessThanEquals(Callable<Eq<Integer>> eq, Callable<Integer> left, Callable<Integer> right) {
        return callable(() -> eq.call().eq(left, right).call() || left.call() < right.call());
    }
}
