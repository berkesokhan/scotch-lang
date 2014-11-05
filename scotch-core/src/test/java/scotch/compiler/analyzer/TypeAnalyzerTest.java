package scotch.compiler.analyzer;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.syntax.DefinitionReference.valueRef;
import static scotch.compiler.syntax.StubResolver.defaultMinus;
import static scotch.compiler.syntax.StubResolver.defaultPlus;
import static scotch.compiler.syntax.Type.fn;
import static scotch.compiler.syntax.Type.sum;
import static scotch.compiler.util.TestUtil.analyzeTypes;

import org.junit.Before;
import org.junit.Test;
import scotch.compiler.syntax.StubResolver;
import scotch.compiler.syntax.SymbolTable;

public class TypeAnalyzerTest {

    private StubResolver resolver;

    @Before
    public void setUp() {
        resolver = new StubResolver()
            .define(defaultPlus())
            .define(defaultMinus());
    }

    @Test
    public void identityOfIntShouldBeInt() {
        SymbolTable symbols = analyzeTypes(
            resolver,
            "module scotch.test",
            "id x = x",
            "test = id 5"
        );
        assertThat(symbols.getValue(valueRef("scotch.test", "test")), is(sum("scotch.data.int.Int")));
    }

    @Test
    public void fibShouldBeIntOfInt() {
        SymbolTable symbols = analyzeTypes(
            resolver,
            "module scotch.test",
            "import scotch.data.num",
            "fib 0 = 0",
            "fib 1 = 1",
            "fib n = fib (n - 1) + fib (n - 2)"
        );
        assertThat(symbols.getValue(valueRef("scotch.test", "fib")), is(fn(sum("scotch.data.int.Int"), sum("scotch.data.int.Int"))));
    }
}
