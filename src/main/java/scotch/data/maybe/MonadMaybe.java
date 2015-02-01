package scotch.data.maybe;

import static java.util.Arrays.asList;
import static scotch.compiler.symbol.type.Type.sum;
import static scotch.compiler.symbol.type.Type.var;
import static scotch.data.maybe.Maybe.nothing;
import static scotch.runtime.RuntimeUtil.callable;
import static scotch.runtime.RuntimeUtil.flatCallable;

import java.util.List;
import scotch.compiler.symbol.InstanceGetter;
import scotch.compiler.symbol.TypeInstance;
import scotch.compiler.symbol.TypeParameters;
import scotch.compiler.symbol.type.Type;
import scotch.control.monad.Monad;
import scotch.runtime.Applicable;
import scotch.runtime.Callable;

@SuppressWarnings("unused")
@TypeInstance(typeClass = "scotch.control.monad.Monad")
public class MonadMaybe<A2> implements Monad<Maybe<A2>> {

    private static final Callable<MonadMaybe> INSTANCE = callable(MonadMaybe::new);

    private MonadMaybe() {
        // intentionally empty
    }

    @SuppressWarnings("unchecked")
    @InstanceGetter
    public static <A> Callable<MonadMaybe<A>> instance() {
        return (Callable) INSTANCE;
    }

    @TypeParameters
    public static List<Type> parameters() {
        return asList(sum("scotch.data.either.Either", var("a")));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A, Mb> Callable<Mb> bind(Callable<Maybe<A2>> ma, Applicable<A, Mb> function) {
        return flatCallable(() -> ma.call().map((Applicable) function));
    }

    @Override
    public Callable<Maybe<A2>> fail(Callable<String> message) {
        return nothing();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A> Callable<Maybe<A2>> wrap(Callable<A> a) {
        return flatCallable(() -> {
            Callable<A2> a2 = (Callable<A2>) a;
            return Maybe.<A2>just().apply(a2);
        });
    }
}
