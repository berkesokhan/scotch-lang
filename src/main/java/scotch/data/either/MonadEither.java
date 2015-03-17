package scotch.data.either;

import static java.util.Arrays.asList;
import static scotch.symbol.type.Types.sum;
import static scotch.symbol.type.Types.var;
import static scotch.data.either.Either.left;
import static scotch.data.either.Either.right;
import static scotch.runtime.RuntimeUtil.callable;

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
public class MonadEither implements Monad {

    private static final Callable<MonadEither> INSTANCE = callable(MonadEither::new);

    @InstanceGetter
    public static Callable<MonadEither> instance() {
        return (Callable) INSTANCE;
    }

    @TypeParameters
    public static List<Type> parameters() {
        return asList(
            sum("scotch.data.either.Either", var("a"))
        );
    }

    @Override
    public Callable bind(Callable value, Applicable transformer) {
        return callable(() -> transformer.apply(value));
    }

    @Override
    public Callable fail(Callable message) {
        return callable(() -> left().apply(message));
    }

    @Override
    public Callable wrap(Callable value) {
        return callable(() -> right().apply(value));
    }
}
