package scotch.compiler.symbol;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.symbol.Operator.operator;
import static scotch.compiler.symbol.Symbol.fromString;
import static scotch.compiler.symbol.Type.fn;
import static scotch.compiler.symbol.Type.sum;
import static scotch.compiler.symbol.Value.Fixity.LEFT_INFIX;

import org.junit.Test;

public class ClasspathResolverTest {

    @Test
    public void shouldResolveJavaSymbol() {
        ClasspathResolver resolver = new ClasspathResolver(getClass().getClassLoader());
        Type intType = sum("scotch.data.int.Int");
        SymbolEntry plus = resolver.getEntry(fromString("scotch.data.num.(+)")).get();
        assertThat(plus.getValue(), is(fn(intType, fn(intType, intType))));
        assertThat(plus.getOperator(), is(operator(LEFT_INFIX, 7)));
        assertThat(plus.getValueSignature(), is(new JavaSignature("scotch/data/num/NumModule", "plus", "()Lscotch/runtime/Applicable;")));
    }
}
