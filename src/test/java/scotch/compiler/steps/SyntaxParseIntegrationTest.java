package scotch.compiler.steps;

import static java.util.Arrays.asList;
import static scotch.symbol.Symbol.qualified;
import static scotch.symbol.SymbolEntry.immutableEntry;
import static scotch.symbol.type.Types.fn;
import static scotch.symbol.type.Types.sum;
import static scotch.symbol.type.Types.t;
import static scotch.symbol.type.Types.var;
import static scotch.compiler.syntax.StubResolver.defaultEq;
import static scotch.compiler.syntax.StubResolver.defaultInt;
import static scotch.compiler.syntax.StubResolver.defaultPlus;
import static scotch.compiler.syntax.value.Values.apply;
import static scotch.compiler.util.TestUtil.arg;
import static scotch.compiler.util.TestUtil.capture;
import static scotch.compiler.util.TestUtil.equal;
import static scotch.compiler.util.TestUtil.id;
import static scotch.compiler.util.TestUtil.let;
import static scotch.compiler.util.TestUtil.literal;
import static scotch.compiler.util.TestUtil.pattern;
import static scotch.compiler.util.TestUtil.valueRef;

import java.util.function.Function;
import org.junit.Ignore;
import org.junit.Test;
import scotch.compiler.Compiler;
import scotch.compiler.ParserTest;
import scotch.compiler.syntax.StubResolver;
import scotch.compiler.syntax.definition.DefinitionGraph;
import scotch.compiler.util.TestUtil;

public class SyntaxParseIntegrationTest extends ParserTest {

    @Test
    public void shouldShufflePattern() {
        resolver.define(immutableEntry(qualified("scotch.data.bool", "not")).build());
        parse(
            "module scotch.test",
            "import scotch.data.bool",
            "left infix 6 (==), (/=)",
            "x == y = not (x /= y)"
        );
        shouldHaveValue("scotch.test.(==)", t(12), TestUtil.matcher("scotch.test.(==#0)", t(11), asList(arg("#0", t(9)), arg("#1", t(10))),
            pattern("scotch.test.(==#0#0)", asList(capture("#0", "x", t(0)), capture("#1", "y", t(2))), apply(
                id("scotch.data.bool.not", t(3)),
                apply(
                    apply(id("scotch.test.(/=)", t(5)), id("x", t(4)), t(13)),
                    id("y", t(6)),
                    t(14)
                ),
                t(15)
            ))
        ));
    }

    @Test
    public void shouldConsolidatePatternsIntoSingleValue() {
        parse(
            "module scotch.test",
            "left infix 8 (+), (-)",
            "fib 0 = 0",
            "fib 1 = 1",
            "fib n = fib (n - 1) + fib (n - 2)"
        );
        shouldHaveValue("scotch.test.fib", t(17), TestUtil.matcher("scotch.test.(fib#0)", t(16), asList(arg("#0", t(15))),
            pattern("scotch.test.(fib#0#0)", asList(equal("#0", apply(
                apply(id("scotch.data.eq.(==)", t(18)), id("#0", t(19)), t(20)),
                literal(0),
                t(21)
            ))), literal(0)),
            pattern("scotch.test.(fib#0#1)", asList(equal("#0", apply(
                apply(id("scotch.data.eq.(==)", t(22)), id("#0", t(23)), t(24)),
                literal(1),
                t(25)
            ))), literal(1)),
            pattern("scotch.test.(fib#0#2)", asList(capture("#0", "n", t(3))), apply(
                apply(
                    id("scotch.test.(+)", t(9)),
                    apply(
                        id("scotch.test.fib", t(4)),
                        apply(
                            apply(id("scotch.test.(-)", t(6)), id("n", t(5)), t(26)),
                            literal(1),
                            t(27)
                        ),
                        t(28)
                    ),
                    t(32)
                ),
                apply(
                    id("scotch.test.fib", t(10)),
                    apply(
                        apply(id("scotch.test.(-)", t(12)), id("n", t(11)), t(29)),
                        literal(2),
                        t(30)
                    ),
                    t(31)
                ),
                t(33)
            ))
        ));
    }

    @Test
    public void shouldQualifySiblingValues() {
        resolver
            .define(defaultPlus())
            .define(defaultEq());
        parse(
            "module scotch.test",
            "import scotch.data.eq",
            "import scotch.data.num",
            "fn a b = a + b == b + a",
            "commutative? a b = fn a b"
        );
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.(commutative?)", TestUtil.matcher("scotch.test.(commutative?#0)", t(28), asList(arg("#0", t(26)), arg("#1", t(27))),
            pattern(
                "scotch.test.(commutative?#0#0)",
                asList(capture("#0", "a", t(11)), capture("#1", "b", t(12))),
                apply(
                    apply(
                        id("scotch.test.fn", t(13)),
                        id("a", t(14)),
                        t(30)
                    ),
                    id("b", t(15)),
                    t(31)
                )
            )
        ));
    }

    @Test
    public void shouldParseLet() {
        parse(
            "module scotch.test",
            "left infix 7 (+)",
            "main = let",
            "    f x = a x",
            "    a g = g + g",
            "  f 2"
        );
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.(main#f)", TestUtil.matcher("scotch.test.(main#f#0)", t(13), asList(arg("#0", t(12))), pattern(
            "scotch.test.(main#f#0#0)",
            asList(capture("#0", "x", t(2))),
            apply(id("scotch.test.(main#a)", t(3)), id("x", t(4)), t(15))
        )));
        shouldHaveValue("scotch.test.(main#a)", TestUtil.matcher("scotch.test.(main#a#0)", t(17), asList(arg("#0", t(16))), pattern(
            "scotch.test.(main#a#0#0)",
            asList(capture("#0", "g", t(6))),
            apply(apply(id("scotch.test.(+)", t(8)), id("g", t(7)), t(19)), id("g", t(9)), t(20))
        )));
        shouldHaveValue("scotch.test.main", let(
            "scotch.test.(main#0)",
            asList(valueRef("scotch.test.(main#f)"), valueRef("scotch.test.(main#a)")),
            apply(id("scotch.test.(main#f)", t(10)), literal(2), t(11))
        ));
    }

    @Ignore
    @Test
    public void shouldParseTypeClass() {
        parse(
            "module scotch.test",
            "import scotch.data.bool",
            "class Eq a where",
            "    (==), (/=) :: a -> a -> Bool",
            "    x == y = not $ x /= y",
            "    x /= y = not $ x == y"
        );
        shouldHaveClass("scotch.test.Eq", asList(var("a")), asList(
            valueRef("scotch.test.(==)"),
            valueRef("scotch.test.(/=)"),
            valueRef("scotch.test.(==)"),
            valueRef("scotch.test.(/=)")
        ));
        shouldHaveValue("scotch.test.(==)", fn(var("a", asList("Eq")), fn(var("a", asList("Eq")), sum("Bool"))));
    }

    @Override
    protected void initResolver(StubResolver resolver) {
        resolver.define(defaultInt());
    }

    @Override
    protected Function<scotch.compiler.Compiler, DefinitionGraph> parse() {
        return Compiler::qualifyNames;
    }

    @Override
    protected void setUp() {
        // intentionally empty
    }
}
