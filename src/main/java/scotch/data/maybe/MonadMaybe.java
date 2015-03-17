package scotch.data.maybe;

import static java.util.Arrays.asList;
import static scotch.symbol.type.Types.sum;
import static scotch.data.maybe.Maybe.just;
import static scotch.data.maybe.Maybe.nothing;
import static scotch.runtime.RuntimeSupport.callable;

import java.util.List;
import scotch.symbol.InstanceGetter;
import scotch.symbol.TypeInstance;
import scotch.symbol.TypeParameters;
import scotch.symbol.type.Type;
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
        return asList(sum("scotch.data.maybe.Maybe"));
    }

    @Override
    public Callable bind(Callable value, Applicable transformer) {
        return ((Maybe) value.call()).map(transformer.call());
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
