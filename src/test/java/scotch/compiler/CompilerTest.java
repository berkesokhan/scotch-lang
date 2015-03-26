package scotch.compiler;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.Compiler.compiler;
import static scotch.compiler.syntax.reference.DefinitionReference.signatureRef;
import static scotch.compiler.util.TestUtil.classDef;
import static scotch.compiler.util.TestUtil.classRef;
import static scotch.compiler.util.TestUtil.ctorDef;
import static scotch.compiler.util.TestUtil.dataDef;
import static scotch.compiler.util.TestUtil.dataRef;
import static scotch.compiler.util.TestUtil.valueRef;
import static scotch.symbol.Symbol.symbol;
import static scotch.symbol.type.Types.sum;
import static scotch.util.StringUtil.quote;

import java.net.URI;
import java.util.List;
import java.util.function.Function;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import scotch.compiler.error.SyntaxError;
import scotch.compiler.syntax.definition.DataFieldDefinition;
import scotch.compiler.syntax.definition.DefinitionGraph;
import scotch.compiler.syntax.definition.ValueDefinition;
import scotch.compiler.syntax.definition.ValueSignature;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.syntax.value.Value;
import scotch.symbol.SymbolResolver;
import scotch.symbol.type.Type;

public abstract class CompilerTest<Resolver extends SymbolResolver> {

    @Rule
    public final    TestName testName   = new TestName();
    protected final Type     intType    = sum("scotch.data.int.Int");
    protected final Type     doubleType = sum("scotch.data.double.Double");
    protected final Type     boolType   = sum("scotch.data.bool.Bool");
    protected final Type     stringType = sum("scotch.data.string.String");
    protected DefinitionGraph              graph;
    protected Function<String[], Compiler> compilerFactory;
    protected Resolver                     resolver;

    @Before
    public void setUp() {
        resolver = initResolver();
        compilerFactory = initCompilerFactory();
    }

    protected void compile(String... lines) {
        graph = compile().apply(compilerFactory.apply(lines));
    }

    protected abstract Function<Compiler, DefinitionGraph> compile();

    protected Scope getScope(DefinitionReference reference) {
        return graph.getScope(reference);
    }

    protected ValueDefinition getValueDefinition(String name) {
        return graph.getDefinition(valueRef(name)).get();
    }

    protected Function<String[], Compiler> initCompilerFactory() {
        return lines -> compiler(resolver, URI.create("test://" + testName.getMethodName()), lines);
    }

    protected abstract Resolver initResolver();

    protected void shouldBeDefined(DefinitionReference reference, String name) {
        assertThat(
            "Symbol " + quote(name) + " is not defined in scope " + reference,
            getScope(reference).isDefined(symbol(name)),
            is(true)
        );
    }

    protected void shouldHaveClass(String className, List<Type> arguments, List<DefinitionReference> members) {
        assertThat(graph.getDefinition(classRef(className)).get(), is(
            classDef(className, arguments, members)
        ));
    }

    protected void shouldHaveData(int ordinal, String name, List<Type> parameters, List<DataFieldDefinition> fields) {
        assertThat(graph.hasErrors(), is(false));
        assertThat(graph.getDefinition(dataRef(name)).get(), is(dataDef(name, parameters, asList(ctorDef(ordinal, name, name, fields)))));
    }

    protected void shouldHaveErrors(SyntaxError... errors) {
        assertThat(graph.hasErrors(), is(true));
        assertThat(graph.getErrors(), contains(errors));
    }

    protected void shouldHaveSignature(String name, Type type) {
        assertThat(((ValueSignature) graph.getDefinition(signatureRef(symbol(name))).get()).getType(), is(type));
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

    protected void shouldNotHaveErrors() {
        assertThat(
            "Definition graph has " + graph.getErrors().size() + " errors!\n\t" + graph.getErrors().stream()
                .map(error -> error.prettyPrint() + "\n\tDebugTrace:\n\t\t" + error.getStackTrace().stream()
                    .limit(10)
                    .map(Object::toString)
                    .collect(joining("\n\t\t")) + "\n\t\t...")
                .collect(joining("\n\t")),
            graph.hasErrors(),
            is(false)
        );
    }
}
