package scotch.compiler;

import static java.util.Arrays.asList;
import static scotch.compiler.symbol.Type.t;
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
import scotch.compiler.syntax.StubResolver;
import scotch.compiler.syntax.definition.DefinitionGraph;

public class ParsePrecedenceTest extends ParserTest {

    @Test
    public void shouldShuffleTwoPlusTwo() {
        parse(
            "module scotch.test",
            "left infix 7 (+)",
            "four = 2 + 2"
        );
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.four", apply(
            apply(
                id("scotch.test.(+)", t(1)),
                literal(2),
                t(2)
            ),
            literal(2),
            t(3)
        ));
    }

    @Test
    public void shouldShufflePattern() {
        parse(
            "module scotch.test",
            "right infix 1 ($)",
            "x $ y = x y"
        );
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.($)", fn(
            "scotch.test.($1)",
            asList(arg("$0", t(5)), arg("$1", t(6))),
            patterns(t(7), pattern(
                "scotch.test.($0)",
                asList(capture("$0", "x", t(0)), capture("$1", "y", t(2))),
                apply(id("x", t(3)), id("y", t(4)), t(9))
            ))
        ));
    }

    @Override
    protected void initResolver(StubResolver resolver) {
        // intentionally empty
    }

    @Override
    protected Function<scotch.compiler.Compiler, DefinitionGraph> parse() {
        return Compiler::parsePrecedence;
    }

    @Override
    protected void setUp() {
        // intentionally empty
    }
}
