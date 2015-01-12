package scotch.compiler.parser;

import static java.util.Arrays.asList;
import static scotch.compiler.symbol.Symbol.qualified;
import static scotch.compiler.symbol.SymbolEntry.immutableEntry;
import static scotch.compiler.symbol.Type.fn;
import static scotch.compiler.symbol.Type.sum;
import static scotch.compiler.symbol.Type.t;
import static scotch.compiler.symbol.Type.var;
import static scotch.compiler.syntax.StubResolver.defaultEq;
import static scotch.compiler.syntax.StubResolver.defaultInt;
import static scotch.compiler.syntax.StubResolver.defaultPlus;
import static scotch.compiler.syntax.Value.apply;
import static scotch.compiler.util.TestUtil.arg;
import static scotch.compiler.util.TestUtil.capture;
import static scotch.compiler.util.TestUtil.equal;
import static scotch.compiler.util.TestUtil.fn;
import static scotch.compiler.util.TestUtil.id;
import static scotch.compiler.util.TestUtil.let;
import static scotch.compiler.util.TestUtil.literal;
import static scotch.compiler.util.TestUtil.message;
import static scotch.compiler.util.TestUtil.pattern;
import static scotch.compiler.util.TestUtil.patterns;
import static scotch.compiler.util.TestUtil.scopeRef;
import static scotch.compiler.util.TestUtil.valueRef;

import java.util.function.Function;
import org.junit.Ignore;
import org.junit.Test;
import scotch.compiler.Compiler;
import scotch.compiler.syntax.DefinitionGraph;
import scotch.compiler.syntax.StubResolver;

public class SyntaxParserTest extends ParserTest {

    @Test
    public void shouldShufflePattern() {
        resolver.define(immutableEntry(qualified("scotch.data.bool", "not")).build());
        parse(
            "module scotch.test",
            "import scotch.data.bool",
            "left infix 6 (==), (/=)",
            "x == y = not (x /= y)"
        );
        shouldHaveValue("scotch.test.(==)", t(7), fn("scotch.test.($1)", asList(arg("$0", t(8)), arg("$1", t(9))), patterns(t(10),
            pattern("scotch.test.($0)", asList(capture("$0", "x", t(0)), capture("$1", "y", t(2))), apply(
                id("scotch.data.bool.not", t(3)),
                apply(
                    apply(id("scotch.test.(/=)", t(5)), id("x", t(4)), t(11)),
                    id("y", t(6)),
                    t(12)
                ),
                t(13)
            ))
        )));
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
        shouldHaveValue("scotch.test.fib", t(11), fn("scotch.test.($3)", asList(arg("$0", t(12))), patterns(t(13),
            pattern("scotch.test.($0)", asList(equal("$0", literal(0))), literal(0)),
            pattern("scotch.test.($1)", asList(equal("$0", literal(1))), literal(1)),
            pattern("scotch.test.($2)", asList(capture("$0", "n", t(3))), apply(
                apply(
                    id("scotch.test.(+)", t(7)),
                    apply(
                        id("scotch.test.fib", t(4)),
                        apply(
                            apply(id("scotch.test.(-)", t(6)), id("n", t(5)), t(14)),
                            literal(1),
                            t(15)
                        ),
                        t(16)
                    ),
                    t(20)
                ),
                apply(
                    id("scotch.test.fib", t(8)),
                    apply(
                        apply(id("scotch.test.(-)", t(10)), id("n", t(9)), t(17)),
                        literal(2),
                        t(18)
                    ),
                    t(19)
                ),
                t(21)
            ))
        )));
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
        shouldHaveValue("scotch.test.(commutative?)", fn(
            "scotch.test.($3)",
            asList(arg("$0", t(27)), arg("$1", t(28))),
            patterns(t(29), pattern(
                "scotch.test.($1)",
                asList(capture("$0", "a", t(11)), capture("$1", "b", t(12))),
                apply(
                    apply(
                        id("scotch.test.fn", t(13)),
                        id("a", t(14)),
                        t(30)
                    ),
                    id("b", t(15)),
                    t(31)
                )
            ))
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
        shouldHaveValue("scotch.test.main", let(
            "scotch.test.($2)",
            asList(scopeRef("scotch.test.(main#f)"), scopeRef("scotch.test.(main#a)")),
            message(id("scotch.test.(main#f)", t(10)), literal(2))
        ));
        shouldHaveValue("scotch.test.(main#f)", fn("scotch.test.($4)", asList(arg("$0", t(0))), patterns(t(0), pattern(
            "scotch.test.($5)",
            asList(capture("$0", "x", t(0))),
            apply(id("scotch.test.(main#a)", t(0)), id("x", t(0)), t(0))
        ))));
        shouldHaveValue("scotch.test.(main#a)", fn("scotch.test.($6)", asList(arg("$0", t(0))), patterns(t(0), pattern(
            "scotch.test.($7)",
            asList(capture("$0", "g", t(0))),
            apply(apply(id("scotch.test.(+)", t(0)), id("g", t(0)), t(0)), id("g", t(0)), t(0))
        ))));
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
    protected Function<Compiler, DefinitionGraph> parse() {
        return Compiler::parseDependencies;
    }

    @Override
    protected void setUp() {
        // intentionally empty
    }
}
