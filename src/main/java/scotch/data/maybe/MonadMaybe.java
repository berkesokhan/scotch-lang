package scotch.data.maybe;

import static java.util.Arrays.asList;
import static scotch.compiler.symbol.type.Types.sum;
import static scotch.compiler.symbol.type.Types.var;
import static scotch.data.maybe.Maybe.just;
import static scotch.data.maybe.Maybe.nothing;
import static scotch.runtime.RuntimeUtil.callable;

import java.util.List;
import scotch.compiler.symbol.InstanceGetter;
import scotch.compiler.symbol.TypeInstance;
import scotch.compiler.symbol.TypeParameters;
import scotch.compiler.symbol.type.Type;
import scotch.control.monad.Monad;
import scotch.runtime.Applicable;
import scotch.runtime.Callable;

@SuppressWarnings({ "unused", "unchecked" })
@TypeInstance(typeClass = "scotch.control.monad.Monad")
public class MonadMaybe implements Monad {

    private static final Callable<MonadMaybe> INSTANCE = callable(MonadMaybe::new);

    private MonadMaybe() {
        // intentionally empty
    }

    @SuppressWarnings("unchecked")
    @InstanceGetter
    public static Callable<MonadMaybe> instance() {
        return (Callable) INSTANCE;
    }

    @TypeParameters
    public static List<Type> parameters() {
        return asList(sum("scotch.data.either.Either", var("a")));
    }

    @Override
    public Callable bind(Callable value, Applicable transformer) {
        return callable(() -> ((Maybe) value).map(transformer));
    }

    @Override
    public Callable fail(Callable message) {
        return nothing();
    }

    @Override
    public Callable wrap(Callable value) {
        return callable(() -> just().apply(value));
    }
}
