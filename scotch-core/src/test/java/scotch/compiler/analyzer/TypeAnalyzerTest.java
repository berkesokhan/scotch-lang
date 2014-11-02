package scotch.compiler.analyzer;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.ast.DefinitionReference.valueRef;
import static scotch.compiler.ast.Type.sum;
import static scotch.compiler.util.TestUtil.analyzeTypes;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import scotch.compiler.ast.SymbolResolver;
import scotch.compiler.ast.SymbolTable;

@RunWith(MockitoJUnitRunner.class)
public class TypeAnalyzerTest {

    @Mock
    private SymbolResolver resolver;

    @Test
    public void identityOfIntShouldBeInt() {
        SymbolTable symbols = analyzeTypes(
            resolver,
            "module scotch.test",
            "id x = x",
            "test = id 5"
        );
        assertThat(symbols.getType(valueRef("scotch.test", "test")), is(sum("scotch.data.int.Int")));
    }
}
