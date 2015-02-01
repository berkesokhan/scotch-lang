package scotch.compiler.steps;

import static java.util.Arrays.asList;
import static scotch.compiler.symbol.type.Type.sum;
import static scotch.compiler.symbol.type.Type.t;
import static scotch.compiler.syntax.StubResolver.defaultBind;
import static scotch.compiler.syntax.StubResolver.defaultEither;
import static scotch.compiler.syntax.StubResolver.defaultLeft;
import static scotch.compiler.syntax.StubResolver.defaultMonad;
import static scotch.compiler.syntax.StubResolver.defaultMonadOf;
import static scotch.compiler.syntax.StubResolver.defaultRight;
import static scotch.compiler.syntax.value.Value.apply;
import static scotch.compiler.util.TestUtil.arg;
import static scotch.compiler.util.TestUtil.capture;
import static scotch.compiler.util.TestUtil.fn;
import static scotch.compiler.util.TestUtil.id;
import static scotch.compiler.util.TestUtil.literal;
import static scotch.compiler.util.TestUtil.pattern;
import static scotch.compiler.util.TestUtil.patterns;

import java.util.function.Function;
import org.junit.Test;
import scotch.compiler.Compiler;
import scotch.compiler.ParserTest;
import scotch.compiler.syntax.StubResolver;
import scotch.compiler.syntax.definition.DefinitionGraph;

public class QualifyNamesTest extends ParserTest {

    @Test
    public void shouldQualifyNames() {
        parse(
            "module scotch.test",
            "fn1 a b = fn2 a b",
            "fn2 a b = a b"
        );
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.fn1", fn(
            "scotch.test.(fn1#0)",
            asList(arg("#0", t(11)), arg("#1", t(12))),
            patterns(t(13), pattern(
                "scotch.test.(fn1#0#0)",
                asList(capture("#0", "a", t(1)), capture("#1", "b", t(2))),
                apply(
                    apply(id("scotch.test.fn2", t(3)), id("a", t(4)), t(15)),
                    id("b", t(5)),
                    t(16)
                )
            ))
        ));
    }

    @Test
    public void test() {
        parse(
            "module scotch.test",
            "import scotch.control.monad",
            "import scotch.data.either",
            "run = Right \"Yes\" >>= \\which -> Left \"No\""
        );
        shouldHaveValue("scotch.test.run", apply(
            apply(
                id("scotch.control.monad.(>>=)", t(2)),
                apply(id("scotch.data.either.Right", t(1)), literal("Yes"), t(5)),
                t(6)
            ),
            fn("scotch.test.(run#0)", arg("which", t(3)), apply(
                id("scotch.data.either.Left", t(4)),
                literal("No"),
                t(8)
            )),
            t(7)
        ));
    }

    @Override
    protected void initResolver(StubResolver resolver) {
        resolver
            .define(defaultEither())
            .define(defaultRight())
            .define(defaultLeft())
            .define(defaultMonad())
            .define(defaultMonadOf(sum("scotch.data.either.Either", sum("scotch.data.string.String"))))
            .define(defaultBind());
    }

    @Override
    protected Function<Compiler, DefinitionGraph> parse() {
        return Compiler::qualifyNames;
    }

    @Override
    protected void setUp() {
        // intentionally empty
    }
}
