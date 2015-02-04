package scotch.data.show;

import static java.util.Arrays.asList;
import static scotch.compiler.symbol.type.Types.sum;
import static scotch.runtime.RuntimeUtil.callable;

import java.util.List;
import scotch.compiler.symbol.InstanceGetter;
import scotch.compiler.symbol.TypeInstance;
import scotch.compiler.symbol.TypeParameters;
import scotch.compiler.symbol.type.Type;
import scotch.runtime.Callable;
import scotch.runtime.Thunk;

@SuppressWarnings("unused")
@TypeInstance(typeClass = "scotch.data.show.Show")
public class ShowInt implements Show<Integer> {

    private static final Callable<ShowInt> INSTANCE = callable(ShowInt::new);

    @InstanceGetter
    public static Callable<ShowInt> instance() {
        return INSTANCE;
    }

    @TypeParameters
    public static List<Type> parameters() {
        return asList(sum("scotch.data.int.Int"));
    }

    private ShowInt() {
        // intentionally empty
    }

    @Override
    public Callable<String> show(Callable<Integer> operand) {
        return new Thunk<String>() {
            @Override
            protected String evaluate() {
                return String.valueOf(operand.call());
            }
        };
    }
}
