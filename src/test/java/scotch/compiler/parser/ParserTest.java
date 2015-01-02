package scotch.compiler.parser;

import static java.util.stream.Collectors.joining;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.Compiler.compiler;
import static scotch.compiler.syntax.DefinitionReference.signatureRef;
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
import scotch.compiler.syntax.Definition.ValueDefinition;
import scotch.compiler.syntax.Definition.ValueSignature;
import scotch.compiler.syntax.DefinitionGraph;
import scotch.compiler.syntax.DefinitionReference;
import scotch.compiler.syntax.Scope;
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

    protected Scope getScope(DefinitionReference reference) {
        return graph.getScope(reference);
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

    protected void shouldBeDefined(DefinitionReference reference, String name) {
        assertThat(
            "Symbol " + quote(name) + " is not defined in scope " + reference,
            getScope(reference).isDefined(Symbol.fromString(name)),
            is(true)
        );
    }

    protected void shouldHaveClass(String className, List<Type> arguments, List<DefinitionReference> members) {
        assertThat(graph.getDefinition(classRef(className)).get(), is(
            classDef(className, arguments, members)
        ));
    }

    protected void shouldHaveErrors(SyntaxError... errors) {
        assertThat(graph.hasErrors(), is(true));
        assertThat(graph.getErrors(), contains(errors));
    }

    protected void shouldHaveSignature(String name, Type type) {
        assertThat(((ValueSignature) graph.getDefinition(signatureRef(Symbol.fromString(name))).get()).getType(), is(type));
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
}
