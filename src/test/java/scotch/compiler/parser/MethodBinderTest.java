package scotch.compiler.parser;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.symbol.Type.fn;
import static scotch.compiler.symbol.Type.t;
import static scotch.compiler.syntax.StubResolver.defaultInt;
import static scotch.compiler.syntax.StubResolver.defaultMinus;
import static scotch.compiler.syntax.StubResolver.defaultNum;
import static scotch.compiler.syntax.StubResolver.defaultNumOf;
import static scotch.compiler.syntax.StubResolver.defaultPlus;
import static scotch.compiler.syntax.StubResolver.defaultString;
import static scotch.compiler.syntax.Value.apply;
import static scotch.compiler.util.TestUtil.bindMethods;
import static scotch.compiler.util.TestUtil.bodyOf;
import static scotch.compiler.util.TestUtil.boundMethod;
import static scotch.compiler.util.TestUtil.capture;
import static scotch.compiler.util.TestUtil.id;
import static scotch.compiler.util.TestUtil.instanceRef;
import static scotch.compiler.util.TestUtil.intType;
import static scotch.compiler.util.TestUtil.literal;
import static scotch.compiler.util.TestUtil.pattern;
import static scotch.compiler.util.TestUtil.patterns;
import static scotch.compiler.util.TestUtil.unboundMethod;
import static scotch.compiler.util.TestUtil.valueRef;

import org.junit.Before;
import org.junit.Test;
import scotch.compiler.symbol.Type;
import scotch.compiler.syntax.DefinitionGraph;
import scotch.compiler.syntax.StubResolver;

public class MethodBinderTest {

    private Type         intType;
    private StubResolver resolver;

    @Before
    public void setUp() {
        intType = intType();
        resolver = new StubResolver()
            .define(defaultPlus())
            .define(defaultMinus())
            .define(defaultInt())
            .define(defaultString())
            .define(defaultNum())
            .define(defaultNumOf(intType));
    }

    @Test
    public void shouldBindInstanceOfPlus() {
        DefinitionGraph graph = bindMethods(
            resolver,
            "module scotch.test",
            "import scotch.data.num",
            "val = 2 + 2"
        );
        assertThat(graph.getErrors(), empty());
        assertThat(bodyOf(graph.getDefinition(valueRef("scotch.test.val"))), is(
            apply(
                apply(
                    boundMethod(
                        "scotch.data.num.(+)",
                        instanceRef("scotch.data.num", "scotch.data.num.Num", asList(intType)),
                        fn(intType, fn(intType, intType))
                    ),
                    literal(2),
                    fn(intType, intType)
                ),
                literal(2),
                intType
            )
        ));
    }

    @Test
    public void shouldNotBindInstance() {
        DefinitionGraph graph = bindMethods(
            resolver,
            "module scotch.test",
            "import scotch.data.num",
            "fn a b = a + b"
        );
        Type t11 = t(11, asList("scotch.data.num.Num"));
        assertThat(graph.getErrors(), empty());
        assertThat(bodyOf(graph.getDefinition(valueRef("scotch.test.fn"))), is(patterns(
            fn(t11, fn(t11, t11)), pattern("scotch.test.(pattern#3)", asList(capture("a", t11), capture("b", t11)), apply(
                apply(
                    unboundMethod("scotch.data.num.(+)", fn(t11, fn(t11, t11))),
                    id("a", t11),
                    fn(t11, t11)
                ),
                id("b", t11),
                t11
            ))
        )));
    }
}
