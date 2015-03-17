package scotch.symbol.type;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static scotch.symbol.Symbol.symbol;
import static scotch.symbol.type.Types.fn;
import static scotch.symbol.type.Types.t;
import static scotch.symbol.type.Unification.mismatch;
import static scotch.symbol.type.Unification.unified;
import static scotch.symbol.type.Types.ctor;
import static scotch.symbol.type.Types.sum;
import static scotch.symbol.type.Types.var;

import java.util.HashMap;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import scotch.symbol.util.SymbolGenerator;
import scotch.symbol.SymbolResolver;
import scotch.compiler.syntax.scope.DefaultTypeScope;

public class ConstructorTypeTest {

    private DefaultTypeScope scope;

    @Before
    public void setUp() {
        scope = new DefaultTypeScope(new SymbolGenerator(), mock(SymbolResolver.class));
    }

    @Test
    public void constructorShouldFlattenIntoSumIfHeadIsSum() {
        assertThat(ctor(sum("Map", var("k")), var("a")).flatten(), is(sum("Map", var("k"), var("a"))));
    }

    @Test
    public void constructorWithVariableHeadShouldNotFlatten() {
        assertThat(ctor(var("m"), var("a")).flatten(), is(ctor(var("m"), var("a"))));
    }

    @Test
    public void shouldFlattenSubConstructors() {
        assertThat(fn(var("a"), ctor(sum("List"), var("a"))).flatten(), is(fn(var("a"), sum("List", var("a")))));
    }

    @Test
    public void sumShouldUnifyIfItImplementsConstructorContext() {
        scope.implement(symbol("Monad"), sum("List"));
        assertThat(ctor(var("m", asList("Monad")), var("a")).unify(sum("List", var("b")), scope), is(unified(sum("List", var("a")))));
    }

    @Test
    public void sumShouldNotUnifyIfItDoesNotImplementConstructorContext() {
        assertThat(ctor(var("m", asList("Monad")), var("a")).unify(sum("List", var("b")), scope),
            is(mismatch(var("m", asList("Monad")), sum("List", var("b")))));
    }

    @Test
    public void constructorShouldUnifyParameterizedSum() {
        scope.implement(symbol("Monad"), sum("Map", var("k")));
        assertThat(ctor(var("m", asList("Monad")), var("a")).unify(sum("Map", var("k"), var("a")), scope),
            is(unified(sum("Map", var("k"), var("a")))));
    }

    @Test
    public void constructorShouldUnifyWhenPartiallyApplied() {
        scope.implement(symbol("Monad"), sum("Map", var("k")));
        assertThat(ctor(sum("Map", var("k")), var("a")).unify(sum("Map", var("x"), var("y")), scope),
            is(unified(sum("Map", var("k"), var("a")))));
    }

    @Test
    public void constructorShouldNotUnifyWhenSumParameterDoesNotImplementContext() {
        scope.implement(symbol("Toast"), sum("Map", var("k", asList("Eq"))));
        assertThat(ctor(var("a", asList("Toast")), var("m")).unify(sum("Map", sum("Int"), var("y")), scope),
            is(mismatch(var("a", asList("Toast")), sum("Map", sum("Int"), var("y")))));
    }

    @Test
    public void shouldZipWithSum() {
        scope.implement(symbol("Monad"), sum("Either", var("a")));
        assertThat(ctor(var("m", asList("Monad")), var("a")).zip(sum("Either", sum("String"), var("x")), scope),
            is(Optional.of(new HashMap<Type, Type>() {{
                put(var("m"), sum("Either", sum("String")));
                put(var("a"), var("x"));
            }})));
    }

    @Test
    public void shouldReplaceConstructorsWithSumsAsTheyAreInferenced() {
        scope.implement(symbol("Monad"), sum("Either", var("a")));
        Type function = fn(ctor(t(9, asList("Monad")), t(10)), fn(fn(t(10), ctor(t(9, asList("Monad")), t(11))), ctor(t(9, asList("Monad")), t(11))));
        Unification unification = fn(sum("Either", t(13), sum("Int")), t(6)).unify(function, scope);
        assertThat(unification.map(t -> unified(scope.generate(t))), is(unified(
            fn(sum("Either", t(13), sum("Int")), fn(fn(sum("Int"), sum("Either", t(13), t(11))), sum("Either", t(13), t(11))))
        )));
    }
}
