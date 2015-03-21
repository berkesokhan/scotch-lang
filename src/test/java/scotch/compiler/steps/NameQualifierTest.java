package scotch.compiler.steps;

import static java.util.Arrays.asList;
import static scotch.compiler.syntax.StubResolver.defaultBind;
import static scotch.compiler.syntax.StubResolver.defaultEither;
import static scotch.compiler.syntax.StubResolver.defaultLeft;
import static scotch.compiler.syntax.StubResolver.defaultMonad;
import static scotch.compiler.syntax.StubResolver.defaultMonadOf;
import static scotch.compiler.syntax.StubResolver.defaultRight;
import static scotch.compiler.syntax.value.Values.apply;
import static scotch.compiler.util.TestUtil.arg;
import static scotch.compiler.util.TestUtil.capture;
import static scotch.compiler.util.TestUtil.id;
import static scotch.compiler.util.TestUtil.literal;
import static scotch.compiler.util.TestUtil.matcher;
import static scotch.compiler.util.TestUtil.pattern;
import static scotch.symbol.type.Types.sum;
import static scotch.symbol.type.Types.t;

import java.util.function.Function;
import org.junit.Test;
import scotch.compiler.Compiler;
import scotch.compiler.IsolatedCompilerTest;
import scotch.compiler.syntax.definition.DefinitionGraph;

public class NameQualifierTest extends IsolatedCompilerTest {

    @Override
    public void setUp() {
        super.setUp();
        resolver
            .define(defaultEither())
            .define(defaultRight())
            .define(defaultLeft())
            .define(defaultMonad())
            .define(defaultMonadOf(sum("scotch.data.either.Either", sum("scotch.data.string.String"))))
            .define(defaultBind());
    }

    @Test
    public void shouldQualifyNames() {
        compile(
            "module scotch.test",
            "fn1 a b = fn2 a b",
            "fn2 a b = a b"
        );
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.fn1",
            matcher("scotch.test.(fn1#0)", t(13), asList(arg("#0", t(11)), arg("#1", t(12))), pattern(
                "scotch.test.(fn1#0#0)",
                asList(capture("#0", "a", t(1)), capture("#1", "b", t(2))),
                apply(
                    apply(id("scotch.test.fn2", t(3)), id("a", t(4)), t(14)),
                    id("b", t(5)),
                    t(15)
                )
            ))
        );
    }

    @Test
    public void shouldQualifyBindNames() {
        compile(
            "module scotch.test",
            "import scotch.control.monad",
            "import scotch.data.either",
            "run = Right \"Yes\" >>= \\which -> Left \"No\""
        );
        shouldHaveValue("scotch.test.run", apply(
            apply(
                id("scotch.control.monad.(>>=)", t(1)),
                apply(id("scotch.data.either.Right", t(0)), literal("Yes"), t(6)),
                t(7)
            ),
            matcher("scotch.test.(run#0)", t(2), arg("#0", t(4)), pattern(
                "scotch.test.(run#0#1)", asList(capture("#0", "which", t(3))),
                apply(id("scotch.data.either.Left", t(5)), literal("No"), t(9))
            )),
            t(8)
        ));
    }

    @Override
    protected Function<Compiler, DefinitionGraph> compile() {
        return Compiler::qualifyNames;
    }
}
