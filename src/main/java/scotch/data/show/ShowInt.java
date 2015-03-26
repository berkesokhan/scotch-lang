package scotch.data.show;

import static java.util.Arrays.asList;
import static scotch.runtime.RuntimeSupport.callable;

import java.util.List;
import scotch.data.int_.Int;
import scotch.runtime.Callable;
import scotch.runtime.Thunk;
import scotch.symbol.InstanceGetter;
import scotch.symbol.TypeInstance;
import scotch.symbol.TypeParameters;
import scotch.symbol.type.Type;

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
        return asList(Int.TYPE);
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
