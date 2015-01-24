package scotch.compiler.symbol.type;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.symbol.type.Type.fn;
import static scotch.compiler.symbol.type.Type.sum;
import static scotch.compiler.symbol.type.Type.var;
import static scotch.compiler.symbol.type.Type.varSum;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import scotch.compiler.symbol.DataTypeDescriptor;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.SymbolGenerator;
import scotch.compiler.syntax.scope.DefaultTypeScope;

public class VariableSumTest {

    private StubbedTypeScope scope;

    @Before
    public void setUp() {
        scope = new StubbedTypeScope();
    }

    @Test
    public void shouldApplyTypeHavingSameNumberOfParameters() {
        defineDescriptor("List", asList(var("a")));
        shouldApply(varSum("m", var("a")), sum("List", var("a")), sum("List", var("a")));
    }

    private void shouldApply(Type target, SumType argument, Type result) {
        Type actual = target.apply(argument, scope)
            .orElseThrow(unification -> new AssertionError(unification.prettyPrint()));
        assertThat(actual, is(result));
    }

    @Test
    public void shouldApplyTypeHavingMoreParameters() {
        defineDescriptor("Map", asList(var("a"), var("b")));
        shouldApply(varSum("m", var("a")), sum("Map", var("k")), sum("Map", var("a"), var("b")));
    }

    @Test
    public void shouldApplyTypeToFunction() {
        defineDescriptor("Maybe", asList(var("a")));
        shouldApply(
            fn(varSum("m", var("a")), fn(fn(var("a"), varSum("m", var("b"))), varSum("m", var("b")))),
            sum("Maybe"),
            fn(sum("Maybe", var("a")), fn(fn(var("a"), sum("Maybe", var("b"))), sum("Maybe", var("b"))))
        );
    }

    private DataTypeDescriptor defineDescriptor(String name, List<Type> parameters) {
        Symbol symbol = Symbol.fromString(name);
        DataTypeDescriptor descriptor = DataTypeDescriptor.builder(symbol)
            .withParameters(parameters)
            .withClassName("List")
            .withConstructors(emptyList())
            .build();
        scope.define(descriptor);
        return descriptor;
    }

    private static class StubbedTypeScope extends DefaultTypeScope {

        private final Map<Symbol, DataTypeDescriptor> descriptors;

        public StubbedTypeScope() {
            super(new SymbolGenerator());
            descriptors = new HashMap<>();
        }

        public void define(DataTypeDescriptor descriptor) {
            descriptors.put(descriptor.getSymbol(), descriptor);
        }

        @Override
        public DataTypeDescriptor getDataType(Symbol symbol) {
            return descriptors.get(symbol);
        }
    }
}
