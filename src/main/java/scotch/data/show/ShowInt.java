package scotch.data.show;

import static java.util.Arrays.asList;
import static scotch.symbol.type.Types.sum;
import static scotch.runtime.RuntimeSupport.callable;

import java.util.List;
import scotch.symbol.InstanceGetter;
import scotch.symbol.TypeInstance;
import scotch.symbol.TypeParameters;
import scotch.symbol.type.Type;
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
