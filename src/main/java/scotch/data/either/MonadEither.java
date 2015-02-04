package scotch.data.either;

import static java.util.Arrays.asList;
import static scotch.compiler.symbol.type.Types.sum;
import static scotch.runtime.RuntimeUtil.callable;
import static scotch.runtime.RuntimeUtil.flatCallable;

import java.util.List;
import scotch.compiler.symbol.InstanceGetter;
import scotch.compiler.symbol.TypeInstance;
import scotch.compiler.symbol.TypeParameters;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.symbol.type.Types;
import scotch.control.monad.Monad;
import scotch.runtime.Applicable;
import scotch.runtime.Callable;

@SuppressWarnings("unused")
@TypeInstance(typeClass = "scotch.control.monad.Monad")
public class MonadEither<B> implements Monad<Either<String, B>> {

    private static final Callable<MonadEither> INSTANCE = callable(MonadEither::new);

    @SuppressWarnings("unchecked")
    @InstanceGetter
    public static <B> Callable<MonadEither<B>> instance() {
        return (Callable) INSTANCE;
    }

    @TypeParameters
    public static List<Type> parameters() {
        return asList(
            Types.sum("scotch.data.either.Either", Types.sum("scotch.data.string.String"))
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A, Mb> Callable<Mb> bind(Callable<Either<String, B>> ma, Applicable<A, Mb> function) {
        return callable(() -> (Mb) ma.call().map((Applicable) function));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Callable<Either<String, B>> fail(Callable<String> message) {
        return flatCallable(() -> Either.<String, B>left().apply((Callable) message));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A> Callable<Either<String, B>> wrap(Callable<A> a) {
        return flatCallable(() -> Either.<String, B>right().apply((Callable) a));
    }
}
