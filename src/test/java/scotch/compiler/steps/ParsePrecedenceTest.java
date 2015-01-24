package scotch.compiler.steps;

import static java.util.Arrays.asList;
import static scotch.compiler.symbol.type.Type.t;
import static scotch.compiler.syntax.value.Value.apply;
import static scotch.compiler.util.TestUtil.arg;
import static scotch.compiler.util.TestUtil.capture;
import static scotch.compiler.util.TestUtil.equal;
import static scotch.compiler.util.TestUtil.fn;
import static scotch.compiler.util.TestUtil.id;
import static scotch.compiler.util.TestUtil.let;
import static scotch.compiler.util.TestUtil.literal;
import static scotch.compiler.util.TestUtil.pattern;
import static scotch.compiler.util.TestUtil.patterns;
import static scotch.compiler.util.TestUtil.valueRef;

import java.util.function.Function;
import org.junit.Test;
import scotch.compiler.*;
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
            "scotch.test.($#0)",
            asList(arg("#0", t(5)), arg("#1", t(6))),
            patterns(t(7), pattern(
                "scotch.test.($#0#0)",
                asList(capture("#0", "x", t(0)), capture("#1", "y", t(2))),
                apply(id("x", t(3)), id("y", t(4)), t(9))
            ))
        ));
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
        shouldHaveValue("scotch.test.(main#f)", fn("scotch.test.(main#f#0)", arg("#0", t(12)), patterns(t(13), pattern(
            "scotch.test.(main#f#0#0)",
            asList(capture("#0", "x", t(2))),
            apply(id("a", t(3)), id("x", t(4)), t(15))
        ))));
        shouldHaveValue("scotch.test.(main#a)", fn("scotch.test.(main#a#0)", arg("#0", t(16)), patterns(t(17), pattern(
            "scotch.test.(main#a#0#0)",
            asList(capture("#0", "g", t(6))),
            apply(apply(id("scotch.test.(+)", t(8)), id("g", t(7)), t(19)), id("g", t(9)), t(20))
        ))));
        shouldHaveValue("scotch.test.main", let(
            "scotch.test.(main#0)",
            asList(valueRef("scotch.test.(main#f)"), valueRef("scotch.test.(main#a)")),
            apply(id("f", t(10)), literal(2), t(11))
        ));
    }

    @Test
    public void shouldTranslateEqualMatchToUseEq() {
        parse(
            "module scotch.test",
            "fib 0 = 0"
        );
        shouldHaveValue("scotch.test.fib", fn(
            "scotch.test.fib#0",
            arg("#0", t(1)),
            patterns(t(2), pattern(
                "scotch.test.fib#0#0",
                asList(equal("#0", apply(
                    apply(id("scotch.data.eq.(==)", t(4)), id("#0", t(5)), t(6)),
                    literal(0),
                    t(7)
                ))),
                literal(0)
            ))
        ));
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
