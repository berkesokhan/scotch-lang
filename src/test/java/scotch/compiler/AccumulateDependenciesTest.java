package scotch.compiler;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.syntax.StubResolver.defaultPlus;
import static scotch.compiler.syntax.definition.DefinitionGraph.cyclicDependency;
import static scotch.compiler.util.TestUtil.valueRef;

import java.util.List;
import java.util.function.Function;
import org.junit.Test;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.syntax.StubResolver;
import scotch.compiler.syntax.definition.DefinitionGraph;
import scotch.compiler.syntax.definition.DependencyCycle;
import scotch.compiler.util.TestUtil;

public class AccumulateDependenciesTest extends ParserTest {

    @Test
    public void shouldAccumulateDependencies() {
        parse(
            "module scotch.test1",
            "import scotch.test2",
            "import scotch.test3",
            "fn1 x = a x",
            "b = \\x -> x",
            "",
            "module scotch.test2",
            "import scotch.test1",
            "import scotch.test3",
            "fn2 y = c b y",
            "a = \\y -> y",
            "",
            "module scotch.test3",
            "import scotch.test2",
            "c = \\z -> a"
        );
        shouldNotHaveErrors();
        shouldHaveDependencies("scotch.test1.fn1", asList("scotch.test2.a"));
        shouldHaveDependencies("scotch.test2.fn2", asList("scotch.test3.c", "scotch.test1.b"));
        shouldHaveDependencies("scotch.test3.c", asList("scotch.test2.a"));
    }

    @Test
    public void shouldNotGatherExternalDependencies() {
        parse(
            "module scotch.test1",
            "import scotch.test2",
            "import scotch.data.num",
            "fn a b = double a + double b",
            "",
            "module scotch.test2",
            "import scotch.data.num",
            "double x = x + x"
        );
        shouldNotHaveErrors();
        shouldHaveDependencies("scotch.test1.fn", asList("scotch.test2.double"));
        shouldNotHaveDependencies("scotch.test2.double");
    }

    @Test
    public void shouldOrderDependencies() {
        parse(
            "module scotch.test1",
            "import scotch.test2",
            "import scotch.test3",
            "fn1 x = a x",
            "b = \\x -> x",
            "",
            "module scotch.test2",
            "import scotch.test1",
            "import scotch.test3",
            "fn2 y = c b y",
            "a = \\y -> y",
            "",
            "module scotch.test3",
            "import scotch.test2",
            "c = \\z -> a"
        );
        shouldNotHaveErrors();
        shouldHaveDependencies(asList(
            "scotch.test1.b",
            "scotch.test2.a",
            "scotch.test3.c",
            "scotch.test1.fn1",
            "scotch.test2.fn2"
        ));
    }

    @Test
    public void shouldReportCyclicDependency() {
        parse(
            "module scotch.test1",
            "import scotch.test2",
            "import scotch.test3",
            "fn1 x = a x",
            "b = \\x -> fn2 x",
            "",
            "module scotch.test2",
            "import scotch.test1",
            "import scotch.test3",
            "fn2 y = c b y",
            "a = \\y -> y",
            "",
            "module scotch.test3",
            "import scotch.test2",
            "c = \\z -> a"
        );
        shouldHaveErrors(cyclicDependency(DependencyCycle.builder()
            .addNode(Symbol.fromString("scotch.test1.b"), asList(Symbol.fromString("scotch.test2.fn2")))
            .addNode(Symbol.fromString("scotch.test2.fn2"), asList(Symbol.fromString("scotch.test1.b")))
            .build()));
    }

    @Test
    public void shouldOrderDependenciesWithinLet() {
        parse(
            "module scotch.test",
            "import scotch.data.num",
            "main = let",
            "    f x = a x",
            "    a g = g + g",
            "  f 2"
        );
        shouldNotHaveErrors();
        shouldNotHaveDependencies("scotch.test.(main#a)");
        shouldHaveDependencies("scotch.test.(main#f)", asList("scotch.test.(main#a)"));
    }

    private void shouldHaveDependencies(String name, List<String> dependencies) {
        assertThat(getScope(valueRef(name)).getDependencies(), is(dependencies.stream().map(Symbol::fromString).collect(toSet())));
    }

    private void shouldHaveDependencies(List<String> dependencies) {
        assertThat(graph.getValues(), is(dependencies.stream().map(TestUtil::valueRef).collect(toList())));
    }

    private void shouldNotHaveDependencies(String name) {
        assertThat(getScope(valueRef(name)).getDependencies(), is(empty()));
    }

    @Override
    protected void initResolver(StubResolver resolver) {
        resolver.define(defaultPlus());
    }

    @Override
    protected Function<Compiler, DefinitionGraph> parse() {
        return Compiler::accumulateDependencies;
    }

    @Override
    protected void setUp() {
        // intentionally empty
    }
}
