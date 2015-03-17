package scotch.compiler.steps;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.symbol.type.Types.sum;
import static scotch.symbol.type.Unification.mismatch;
import static scotch.compiler.syntax.TypeError.typeError;
import static scotch.compiler.text.SourcePoint.point;
import static scotch.compiler.text.SourceRange.source;
import static scotch.compiler.util.TestUtil.intType;
import static scotch.compiler.util.TestUtil.integratedParse;
import static scotch.compiler.util.TestUtil.stringType;
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
            "import scotch.data.int",
            "import scotch.data.tuple",
            "",
            "tuple = (1, 2, 3)"
        );
        assertThat(graph.hasErrors(), is(false));
        assertThat(graph.getValue(valueRef("scotch.test.tuple")).get(), is(sum("scotch.data.tuple.(,,)", asList(intType(), intType(), intType()))));
    }

    @Test
    public void shouldHaveError_whenStringIsHeterogeneous() {
        parse(
            "module scotch.test",
            "import scotch.data.int",
            "import scotch.data.list",
            "import scotch.data.string",
            "",
            "list = [1, 2, \"oops\"]"
        );
        assertThat(graph.getErrors(), contains(typeError(
            mismatch(intType(), stringType()),
            source("test://shouldHaveError_whenStringIsHeterogeneous", point(107, 6, 15), point(113, 6, 21))
        )));
    }

    @Test
    public void shouldDetermineTypeOfSuccessfulChainedMaybe() {
        parse(
            "module scotch.test",
            "import scotch.control.monad",
            "import scotch.data.function",
            "import scotch.data.int",
            "import scotch.data.maybe",
            "import scotch.data.num",
            "",
            "addedStuff = do",
            "    x <- Just 3",
            "    y <- Just 2",
            "    return $ x + y"
        );
        assertThat(graph.getErrors(), is(empty()));
        assertThat(graph.getValue(valueRef("scotch.test.addedStuff")), is(Optional.of(sum("scotch.data.maybe.Maybe", asList(intType())))));
    }

    @Test
    public void shouldDetermineTypeOfFailedChainedMaybe() {
        parse(
            "module scotch.test",
            "import scotch.control.monad",
            "import scotch.data.function",
            "import scotch.data.int",
            "import scotch.data.maybe",
            "import scotch.data.num",
            "",
            "addedStuff = do",
            "    x <- Just 3",
            "    y <- Nothing",
            "    return $ x + y"
        );
        assertThat(graph.getErrors(), is(empty()));
        assertThat(graph.getValue(valueRef("scotch.test.addedStuff")), is(Optional.of(sum("scotch.data.maybe.Maybe", asList(intType())))));
    }

    private void parse(String... lines) {
        graph = integratedParse(testName.getMethodName(), resolver, lines);
    }
}
