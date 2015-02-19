package scotch.compiler.symbol.type;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static scotch.compiler.symbol.type.Unification.circular;
import static scotch.compiler.symbol.type.Unification.contextMismatch;
import static scotch.compiler.symbol.type.Unification.mismatch;
import static scotch.compiler.symbol.type.Unification.unified;

import java.util.List;
import org.junit.Before;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.SymbolGenerator;
import scotch.compiler.symbol.SymbolResolver;
import scotch.compiler.symbol.TypeScope;
import scotch.compiler.syntax.scope.DefaultTypeScope;

public class UnificationTest {

    protected TypeScope scope;

    @Before
    public void initialize() {
        scope = createTypeScope();
        setUp();
    }

    protected TypeScope createTypeScope() {
        return new DefaultTypeScope(new SymbolGenerator(), mock(SymbolResolver.class));
    }

    protected void setUp() {
        // extension point
    }

    private List<Symbol> symbolize(List<String> list) {
        return list.stream().map(Symbol::symbol).collect(toList());
    }

    protected void addContext(Type type, String... context) {
        scope.extendContext(type, asList(context).stream().map(Symbol::symbol).collect(toSet()));
    }

    protected Type argumentOf(Type type) {
        if (type instanceof FunctionType) {
            return ((FunctionType) type).getArgument();
        } else {
            throw new UnsupportedOperationException("Can't get argument from " + type.getClass().getSimpleName());
        }
    }

    protected void shouldBeBound(Type variable, Type target) {
        assertThat(scope.generate(variable), is(target));
    }

    protected void shouldBeCircular(Unification result, Type target, Type variable) {
        assertFalse(result.isUnified());
        assertThat(result, is(circular(target, variable)));
    }

    protected void shouldBeContextMismatch(Unification unification, Type expected, Type actual, List<String> expectedContext, List<String> actualContext) {
        assertFalse(unification.isUnified());
        assertThat(unification, is(contextMismatch(expected, actual, symbolize(expectedContext), symbolize(actualContext))));
    }

    protected void shouldBeMismatch(Unification result, Type target, Type actual) {
        assertFalse(result.isUnified());
        assertThat(result, is(mismatch(target, actual)));
    }

    protected void shouldBeReplaced(Type query, Type result) {
        assertThat(scope.generate(query), is(result));
    }

    protected void shouldBeUnified(Unification result, Type target) {
        assertTrue(result.isUnified());
        assertThat(result, is(unified(target)));
    }

    protected Unification unify(Type target, Type query) {
        return target.unify(query, scope);
    }
}
