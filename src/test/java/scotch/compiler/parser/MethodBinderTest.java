package scotch.compiler.parser;

import static java.util.Arrays.asList;
import static scotch.compiler.symbol.Type.fn;
import static scotch.compiler.symbol.Type.instance;
import static scotch.compiler.symbol.Type.t;
import static scotch.compiler.syntax.StubResolver.defaultEq;
import static scotch.compiler.syntax.StubResolver.defaultEqClass;
import static scotch.compiler.syntax.StubResolver.defaultInt;
import static scotch.compiler.syntax.StubResolver.defaultMinus;
import static scotch.compiler.syntax.StubResolver.defaultNum;
import static scotch.compiler.syntax.StubResolver.defaultNumOf;
import static scotch.compiler.syntax.StubResolver.defaultPlus;
import static scotch.compiler.syntax.StubResolver.defaultString;
import static scotch.compiler.syntax.Value.apply;
import static scotch.compiler.util.TestUtil.arg;
import static scotch.compiler.util.TestUtil.capture;
import static scotch.compiler.util.TestUtil.fn;
import static scotch.compiler.util.TestUtil.instance;
import static scotch.compiler.util.TestUtil.instanceRef;
import static scotch.compiler.util.TestUtil.intType;
import static scotch.compiler.util.TestUtil.literal;
import static scotch.compiler.util.TestUtil.method;
import static scotch.compiler.util.TestUtil.pattern;
import static scotch.compiler.util.TestUtil.patterns;

import java.util.function.Function;
import org.junit.Test;
import scotch.compiler.Compiler;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.Type.InstanceType;
import scotch.compiler.syntax.DefinitionGraph;
import scotch.compiler.syntax.StubResolver;

public class MethodBinderTest extends ParserTest {

    private Type intType;

    @Test
    public void shouldBindInstanceOfPlus() {
        parse(
            "module scotch.test",
            "import scotch.data.num",
            "val = 2 + 2"
        );
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.val", apply(
            apply(
                apply(
                    method(
                        "scotch.data.num.(+)",
                        asList(instance("scotch.data.num.Num", t(0))),
                        fn(instance("scotch.data.num.Num", t(0)), fn(intType, fn(intType, intType)))
                    ),
                    instance(
                        instanceRef("scotch.data.num", "scotch.data.num.Num", asList(intType)),
                        fn(intType, fn(intType, intType))
                    ),
                    fn(intType, fn(intType, intType))
                ),
                literal(2),
                fn(intType, intType)
            ),
            literal(2),
            intType
        ));
    }

    @Test
    public void shouldBubbleUpMethodBinding() {
        parse(
            "module scotch.test",
            "import scotch.data.num",
            "fn a b = a + b"
        );
        String num = "scotch.data.num.Num";
        Type t = t(12, asList(num));
        InstanceType instance = instance(num, t(12));
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.fn", fn(
            "scotch.test.($1)",
            asList(arg("$i0", instance), arg("$0", t), arg("$1", t)),
            patterns(t, pattern("scotch.test.($0)", asList(capture("$0", "a", t), capture("$1", "b", t)), apply(
                apply(
                    apply(
                        method("scotch.data.num.(+)", asList(instance), fn(instance, fn(t, fn(t, t)))),
                        arg("$i0", instance),
                        fn(t, fn(t, t))
                    ),
                    arg("a", t),
                    fn(t, t)
                ),
                arg("b", t),
                t
            )))
        ));
    }

    @Test
    public void shouldGatherRequiredInstances() {
        parse(
            "module scotch.test",
            "import scotch.data.eq",
            "import scotch.data.num",
            "fn a b = a + b == b + a"
        );
        shouldNotHaveErrors();
        shouldRequireInstances("scotch.test.fn", t(20), asList("scotch.data.eq.Eq", "scotch.data.num.Num"));
    }

    @Override
    protected void initResolver(StubResolver resolver) {
        resolver
            .define(defaultPlus())
            .define(defaultMinus())
            .define(defaultInt())
            .define(defaultString())
            .define(defaultNum())
            .define(defaultNumOf(intType))
            .define(defaultEq())
            .define(defaultEqClass());
    }

    @Override
    protected Function<Compiler, DefinitionGraph> parse() {
        return Compiler::bindMethods;
    }

    @Override
    protected void setUp() {
        intType = intType();
    }
}
