package scotch.compiler.steps;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.syntax.StubResolver.defaultPlus;
import static scotch.compiler.syntax.definition.DefinitionGraph.cyclicDependency;
import static scotch.compiler.util.TestUtil.valueRef;
import static scotch.symbol.Symbol.symbol;

import java.util.List;
import java.util.function.Function;
import org.junit.Before;
import org.junit.Test;
import scotch.compiler.Compiler;
import scotch.compiler.IsolatedCompilerTest;
import scotch.compiler.syntax.definition.DefinitionGraph;
import scotch.compiler.syntax.definition.DependencyCycle;
import scotch.compiler.util.TestUtil;
import scotch.symbol.Symbol;

public class AccumulateDependenciesTest extends IsolatedCompilerTest {

    @Before
    public void setUp() {
        super.setUp();
        resolver.define(defaultPlus());
    }

    @Test
    public void shouldAccumulateDependencies() {
        compile(
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
        compile(
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
        compile(
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
        compile(
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
            .addNode(symbol("scotch.test1.b"), asList(symbol("scotch.test2.fn2")))
            .addNode(symbol("scotch.test2.fn2"), asList(symbol("scotch.test1.b")))
            .build()));
    }

    @Test
    public void shouldOrderDependenciesWithinLet() {
        compile(
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
        assertThat(getScope(valueRef(name)).getDependencies(), is(dependencies.stream().map(Symbol::symbol).collect(toSet())));
    }

    private void shouldHaveDependencies(List<String> dependencies) {
        assertThat(graph.getValues(), is(dependencies.stream().map(TestUtil::valueRef).collect(toList())));
    }

    private void shouldNotHaveDependencies(String name) {
        assertThat(getScope(valueRef(name)).getDependencies(), is(empty()));
    }

    @Override
    protected Function<scotch.compiler.Compiler, DefinitionGraph> compile() {
        return Compiler::accumulateDependencies;
    }
}
