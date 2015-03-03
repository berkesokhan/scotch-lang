package scotch.compiler.steps;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.symbol.type.Types.sum;
import static scotch.compiler.util.TestUtil.intType;
import static scotch.compiler.util.TestUtil.integratedParse;
import static scotch.compiler.util.TestUtil.valueRef;

import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import scotch.compiler.ClassLoaderResolver;
import scotch.compiler.syntax.definition.DefinitionGraph;

public class TypeCheckerIntegrationTest {

    @Rule
    public final TestName testName = new TestName();
    private DefinitionGraph graph;
    private ClassLoaderResolver resolver;

    @Before
    public void setUp() {
        resolver = new ClassLoaderResolver(Optional.empty(), getClass().getClassLoader());
    }

    @Test
    public void shouldHaveTypeOfTuple3OfInts() {
        parse(
            "module scotch.test",
            "tuple = (1, 2, 3)"
        );
        assertThat(graph.getValue(valueRef("scotch.test.tuple")).get(), is(sum("scotch.data.tuple.(,,)", asList(intType(), intType(), intType()))));
    }

    private void parse(String... lines) {
        graph = integratedParse(testName.getMethodName(), resolver, lines);
    }
}
