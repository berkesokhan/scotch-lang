package scotch.data.show;

import static java.util.Arrays.asList;
import static scotch.compiler.symbol.Type.sum;

import java.util.List;
import scotch.compiler.symbol.InstanceGetter;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.TypeInstance;
import scotch.compiler.symbol.TypeParameters;
import scotch.runtime.Callable;
import scotch.runtime.Thunk;

@SuppressWarnings("unused")
@TypeInstance(typeClass = "scotch.data.show.Show")
public class ShowInt implements Show<Integer> {

    private static final ShowInt INSTANCE = new ShowInt();

    @InstanceGetter
    public static ShowInt instance() {
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
