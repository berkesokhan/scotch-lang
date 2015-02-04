package scotch.compiler.symbol.type;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.symbol.Symbol.symbol;
import static scotch.compiler.symbol.type.Types.fn;
import static scotch.compiler.symbol.type.Types.sum;
import static scotch.compiler.symbol.type.Types.var;
import static scotch.compiler.symbol.type.Types.varSum;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import scotch.compiler.symbol.DataTypeDescriptor;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.SymbolGenerator;
import scotch.compiler.symbol.TypeScope;
import scotch.compiler.syntax.scope.DefaultTypeScope;

public class VariableSumTest extends UnificationTest {

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
        // m a ++ Map k => map k a
        defineDescriptor("Map", asList(var("a"), var("b")));
        VariableSum varSum = varSum("m", var("a"));
        SumType sum = sum("Map", var("k"));
        shouldApply(varSum, sum, sum("Map", var("a"), var("b")));
    }

    @Test
    public void shouldApplyTypeToFunction() {
        // m a -> (a -> m b) -> m b ++ Maybe a ==> Maybe a -> (a -> Maybe b) -> Maybe b
        defineDescriptor("Maybe", asList(var("a")));
        FunctionType function = fn(varSum("m", var("a")), fn(fn(var("a"), varSum("m", var("b"))), varSum("m", var("b"))));
        SumType sum = sum("Maybe");
        FunctionType result = fn(sum("Maybe", var("a")), fn(fn(var("a"), sum("Maybe", var("b"))), sum("Maybe", var("b"))));
        shouldApply(function, sum, result);
    }

    @Test
    public void shouldUnifyWithVariable() {
        Type varSum = varSum("m", var("a"));
        Type var = var("b");
        shouldBeUnified(unify(varSum, var), varSum);
    }

    @Test
    public void variableShouldUnify() {
        Type var = var("b");
        Type varSum = varSum("m", var("a"));
        shouldBeUnified(unify(var, varSum), varSum);
    }

    @Test
    public void shouldNotUnifyWithCircularVariable() {
        Type varSum = varSum("m", var("a"));
        Type var = var("a");
        shouldBeCircular(unify(varSum, var), varSum, var);
    }

    @Test
    public void shouldUnifyWithSum() {
        defineDescriptor("Maybe", asList(var("a")));
        Type varSum = varSum("m", var("a"));
        Type sum = sum("Maybe", var("b"));
        shouldBeUnified(unify(varSum, sum), sum("Maybe", var("a")));
    }

    @Test
    public void sumShouldUnify() {
        defineDescriptor("Maybe", asList(var("a")));
        Type sum = sum("Maybe", var("b"));
        Type varSum = varSum("m", var("a"));
        shouldBeUnified(unify(sum, varSum), sum("Maybe", var("a")));
    }

    private DataTypeDescriptor defineDescriptor(String name, List<Type> parameters) {
        Symbol symbol = symbol(name);
        DataTypeDescriptor descriptor = DataTypeDescriptor.builder(symbol)
            .withParameters(parameters)
            .withClassName("List")
            .withConstructors(emptyList())
            .build();
        ((StubbedTypeScope) scope).define(descriptor);
        return descriptor;
    }

    @Override
    protected TypeScope createTypeScope() {
        return new StubbedTypeScope();
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
