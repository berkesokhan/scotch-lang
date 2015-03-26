package scotch.compiler.steps;

import static java.util.Arrays.asList;
import static scotch.compiler.syntax.StubResolver.defaultEq;
import static scotch.compiler.syntax.StubResolver.defaultPlus;
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
import static scotch.symbol.Symbol.qualified;
import static scotch.symbol.SymbolEntry.immutableEntry;
import static scotch.symbol.type.Types.fn;
import static scotch.symbol.type.Types.sum;
import static scotch.symbol.type.Types.t;
import static scotch.symbol.type.Types.var;

import java.util.function.Function;
import org.junit.Ignore;
import org.junit.Test;
import scotch.compiler.Compiler;
import scotch.compiler.IsolatedCompilerTest;
import scotch.compiler.syntax.definition.DefinitionGraph;

public class SyntaxParseIntegrationTest extends IsolatedCompilerTest {

    @Test
    public void shouldShufflePattern() {
        resolver.define(immutableEntry(qualified("scotch.data.bool", "not")).build());
        compile(
            "module scotch.test",
            "import scotch.data.bool",
            "left infix 6 (==), (/=)",
            "x == y = not (x /= y)"
        );
        shouldHaveValue("scotch.test.(==)", matcher("scotch.test.(==#0)", t(11), asList(arg("#0", t(9)), arg("#1", t(10))),
            pattern("scotch.test.(==#0#0)", asList(capture("#0", "x", t(0)), capture("#1", "y", t(2))), apply(
                id("scotch.data.bool.not", t(3)),
                apply(
                    apply(id("scotch.test.(/=)", t(5)), id("x", t(4)), t(12)),
                    id("y", t(6)),
                    t(13)
                ),
                t(14)
            ))
        ));
    }

    @Test
    public void shouldConsolidatePatternsIntoSingleValue() {
        compile(
            "module scotch.test",
            "left infix 8 (+), (-)",
            "fib 0 = 0",
            "fib 1 = 1",
            "fib n = fib (n - 1) + fib (n - 2)"
        );
        shouldHaveValue("scotch.test.fib", matcher("scotch.test.(fib#0)", t(16), asList(arg("#0", t(15))),
            pattern("scotch.test.(fib#0#0)", asList(equal("#0", apply(
                apply(id("scotch.data.eq.(==)", t(17)), id("#0", t(18)), t(19)),
                literal(0),
                t(20)
            ))), literal(0)),
            pattern("scotch.test.(fib#0#1)", asList(equal("#0", apply(
                apply(id("scotch.data.eq.(==)", t(21)), id("#0", t(22)), t(23)),
                literal(1),
                t(24)
            ))), literal(1)),
            pattern("scotch.test.(fib#0#2)", asList(capture("#0", "n", t(3))), apply(
                apply(
                    id("scotch.test.(+)", t(9)),
                    apply(
                        id("scotch.test.fib", t(4)),
                        apply(
                            apply(id("scotch.test.(-)", t(6)), id("n", t(5)), t(25)),
                            literal(1),
                            t(26)
                        ),
                        t(27)
                    ),
                    t(31)
                ),
                apply(
                    id("scotch.test.fib", t(10)),
                    apply(
                        apply(id("scotch.test.(-)", t(12)), id("n", t(11)), t(28)),
                        literal(2),
                        t(29)
                    ),
                    t(30)
                ),
                t(32)
            ))
        ));
    }

    @Test
    public void shouldQualifySiblingValues() {
        resolver
            .define(defaultPlus())
            .define(defaultEq());
        compile(
            "module scotch.test",
            "import scotch.data.eq",
            "import scotch.data.num",
            "fn a b = a + b == b + a",
            "commutative? a b = fn a b"
        );
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.(commutative?)", matcher("scotch.test.(commutative?#0)", t(27), asList(arg("#0", t(25)), arg("#1", t(26))),
            pattern(
                "scotch.test.(commutative?#0#0)",
                asList(capture("#0", "a", t(11)), capture("#1", "b", t(12))),
                apply(
                    apply(
                        id("scotch.test.fn", t(13)),
                        id("a", t(14)),
                        t(28)
                    ),
                    id("b", t(15)),
                    t(29)
                )
            )
        ));
    }

    @Test
    public void shouldParseLet() {
        compile(
            "module scotch.test",
            "left infix 7 (+)",
            "main = let",
            "    f x = a x",
            "    a g = g + g",
            "  f 2"
        );
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.(main#f)", matcher("scotch.test.(main#f#0)", t(12), asList(arg("#0", t(11))), pattern(
            "scotch.test.(main#f#0#0)",
            asList(capture("#0", "x", t(1))),
            apply(id("scotch.test.(main#a)", t(2)), id("x", t(3)), t(13))
        )));
        shouldHaveValue("scotch.test.(main#a)", matcher("scotch.test.(main#a#0)", t(15), asList(arg("#0", t(14))), pattern(
            "scotch.test.(main#a#0#0)",
            asList(capture("#0", "g", t(5))),
            apply(apply(id("scotch.test.(+)", t(7)), id("g", t(6)), t(16)), id("g", t(8)), t(17))
        )));
        shouldHaveValue("scotch.test.main", let(
            "scotch.test.(main#0)",
            asList(valueRef("scotch.test.(main#f)"), valueRef("scotch.test.(main#a)")),
            apply(id("scotch.test.(main#f)", t(9)), literal(2), t(10))
        ));
    }

    @Ignore
    @Test
    public void shouldParseTypeClass() {
        compile(
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
    protected Function<scotch.compiler.Compiler, DefinitionGraph> compile() {
        return Compiler::qualifyNames;
    }
}
