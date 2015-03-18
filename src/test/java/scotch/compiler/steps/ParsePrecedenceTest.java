package scotch.compiler.steps;

import static java.util.Arrays.asList;
import static scotch.compiler.syntax.value.Values.apply;
import static scotch.compiler.util.TestUtil.arg;
import static scotch.compiler.util.TestUtil.capture;
import static scotch.compiler.util.TestUtil.equal;
import static scotch.compiler.util.TestUtil.id;
import static scotch.compiler.util.TestUtil.let;
import static scotch.compiler.util.TestUtil.literal;
import static scotch.compiler.util.TestUtil.matcher;
import static scotch.compiler.util.TestUtil.pattern;
import static scotch.compiler.util.TestUtil.valueRef;
import static scotch.symbol.type.Types.t;

import java.util.function.Function;
import org.junit.Test;
import scotch.compiler.ParserTest;
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
                id("scotch.test.(+)", t(0)),
                literal(2),
                t(1)
            ),
            literal(2),
            t(2)
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
        shouldHaveValue("scotch.test.($)", matcher("scotch.test.($#0)", t(7), asList(arg("#0", t(5)), arg("#1", t(6))), pattern(
            "scotch.test.($#0#0)",
            asList(capture("#0", "x", t(0)), capture("#1", "y", t(2))),
            apply(id("x", t(3)), id("y", t(4)), t(8))
        )));
    }

    @Test
    public void shouldAliasShuffledLetDeclarations() {
        parse(
            "module scotch.test",
            "left infix 7 (+)",
            "main = let",
            "    f x = a x",
            "    a g = g + g",
            "  f 2"
        );
        shouldHaveValue("scotch.test.(main#f)", matcher("scotch.test.(main#f#0)", t(12), arg("#0", t(11)), pattern(
            "scotch.test.(main#f#0#0)",
            asList(capture("#0", "x", t(1))),
            apply(id("a", t(2)), id("x", t(3)), t(13))
        )));
        shouldHaveValue("scotch.test.(main#a)", matcher("scotch.test.(main#a#0)", t(15), arg("#0", t(14)), pattern(
            "scotch.test.(main#a#0#0)",
            asList(capture("#0", "g", t(5))),
            apply(apply(id("scotch.test.(+)", t(7)), id("g", t(6)), t(16)), id("g", t(8)), t(17))
        )));
        shouldHaveValue("scotch.test.main", let(
            "scotch.test.(main#0)",
            asList(valueRef("scotch.test.(main#f)"), valueRef("scotch.test.(main#a)")),
            apply(id("f", t(9)), literal(2), t(10))
        ));
    }

    @Test
    public void shouldTranslateEqualMatchToUseEq() {
        parse(
            "module scotch.test",
            "fib 0 = 0"
        );
        shouldHaveValue("scotch.test.fib", matcher("scotch.test.(fib#0)", t(2), arg("#0", t(1)), pattern(
            "scotch.test.fib#0#0",
            asList(equal("#0", apply(
                apply(id("scotch.data.eq.(==)", t(3)), id("#0", t(4)), t(5)),
                literal(0),
                t(6)
            ))),
            literal(0)
        )));
    }

    @Override
    protected void initResolver(StubResolver resolver) {
        // intentionally empty
    }

    @Override
    protected Function<scotch.compiler.Compiler, DefinitionGraph> parse() {
        return scotch.compiler.Compiler::parsePrecedence;
    }

    @Override
    protected void setUp() {
        // intentionally empty
    }
}
