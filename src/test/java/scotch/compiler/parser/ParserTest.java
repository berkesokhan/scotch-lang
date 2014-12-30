package scotch.compiler.parser;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.Compiler.compiler;
import static scotch.compiler.util.TestUtil.classDef;
import static scotch.compiler.util.TestUtil.classRef;
import static scotch.compiler.util.TestUtil.value;
import static scotch.compiler.util.TestUtil.valueRef;
import static scotch.util.StringUtil.quote;

import java.util.List;
import java.util.function.Function;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import scotch.compiler.Compiler;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.Type.VariableType;
import scotch.compiler.syntax.Definition.ValueDefinition;
import scotch.compiler.syntax.DefinitionGraph;
import scotch.compiler.syntax.DefinitionReference;
import scotch.compiler.syntax.StubResolver;
import scotch.compiler.syntax.SyntaxError;
import scotch.compiler.syntax.Value;

public abstract class ParserTest {

    @Rule
    public final TestName testName = new TestName();
    protected DefinitionGraph              graph;
    protected StubResolver                 resolver;
    protected Function<String[], Compiler> compiler;

    @Before
    public void init() {
        setUp();
        initResolver(resolver = new StubResolver());
        compiler = lines -> compiler(resolver, testName.getMethodName(), lines);
    }

    protected ValueDefinition getValueDefinition(String name) {
        return graph.getDefinition(valueRef(name)).get();
    }

    protected abstract void initResolver(StubResolver resolver);

    protected abstract Function<Compiler, DefinitionGraph> parse();

    protected void parse(String... lines) {
        graph = parse().apply(compiler.apply(lines));
    }

    protected abstract void setUp();

    protected void shouldHaveClass(String className, List<Type> arguments, List<DefinitionReference> members) {
        assertThat(graph.getDefinition(classRef(className)).get(), is(
            classDef(className, arguments, members)
        ));
    }

    protected void shouldHaveErrors(DefinitionGraph graph, SyntaxError... errors) {
        assertThat(graph.hasErrors(), is(true));
        assertThat(graph.getErrors(), contains(errors));
    }

    protected void shouldHaveValue(String name, Type type) {
        shouldHaveValue(name);
        assertThat(graph.getValue(valueRef(name)).get(), is(type));
    }

    protected void shouldHaveValue(String name) {
        assertThat("Graph did not define value " + quote(name), graph.getValue(valueRef(name)).isPresent(), is(true));
    }

    protected void shouldHaveValue(String name, Value body) {
        shouldHaveValue(name);
        assertThat(getValueDefinition(name).getBody(), is(body));
    }

    protected void shouldHaveValue(String name, Type type, Value body) {
        shouldHaveValue(name);
        assertThat(graph.getDefinition(valueRef(name)).get(), is(value(name, type, body)));
    }

    protected void shouldNotHaveErrors() {
        assertThat(
            "Definition graph had errors!\n\t" + graph.getErrors().stream()
                .map(error -> error.prettyPrint() + "\n\tDebugTrace:\n\t\t" + error.getStackTrace().stream()
                    .map(Object::toString)
                    .collect(joining("\n\t\t")))
                .collect(joining("\n\t")),
            graph.hasErrors(),
            is(false)
        );
    }

    protected void shouldRequireInstances(String name, VariableType type, List<String> references) {
        assertThat(getValueDefinition(name).getInstances().getInstances(type), contains(
            references.stream()
                .map(Symbol::fromString)
                .map(DefinitionReference::classRef)
                .collect(toList())
                .toArray()
        ));
    }
}
