package scotch.compiler.parser;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.ast.Definition.value;
import static scotch.compiler.ast.DefinitionReference.valueRef;
import static scotch.compiler.ast.PatternMatch.capture;
import static scotch.compiler.ast.PatternMatch.equal;
import static scotch.compiler.ast.PatternMatcher.pattern;
import static scotch.compiler.ast.Value.apply;
import static scotch.compiler.ast.Value.id;
import static scotch.compiler.ast.Value.literal;
import static scotch.compiler.ast.Value.patterns;
import static scotch.compiler.util.TestUtil.parseAst;
import static scotch.lang.Symbol.fromString;
import static scotch.lang.Symbol.unqualified;
import static scotch.lang.Type.t;

import org.junit.Test;
import scotch.compiler.ast.SymbolTable;

public class AstParserTest {

    @Test
    public void shouldConsolidatePatternsIntoSingleValue() {
        SymbolTable symbols = parseAst(
            "module scotch.test",
            "left infix 8 (+), (-)",
            "fib 0 = 0",
            "fib 1 = 1",
            "fib n = fib (n - 1) + fib (n - 2)"
        );
        assertThat(symbols.getDefinition(valueRef("scotch.test", "fib")), is(
            value(fromString("scotch.test.fib"), t(20), patterns(
                pattern(asList(equal(literal(0, t(1)))), literal(0, t(3))),
                pattern(asList(equal(literal(1, t(5)))), literal(1, t(7))),
                pattern(asList(capture(unqualified("n"), t(9))), apply(
                    apply(
                        id("scotch.test.(+)", t(15)),
                        apply(
                            id("scotch.test.fib", t(11)),
                            apply(
                                apply(id("scotch.test.(-)", t(13)), id("n", t(12)), t(21)),
                                literal(1, t(14)),
                                t(22)
                            ),
                            t(23)
                        ),
                        t(27)
                    ),
                    apply(
                        id("scotch.test.fib", t(16)),
                        apply(
                            apply(id("scotch.test.(-)", t(18)), id("n", t(17)), t(24)),
                            literal(2, t(19)),
                            t(25)
                        ),
                        t(26)
                    ),
                    t(28)
                ))
            ))
        ));
    }

    @Test
    public void shouldShufflePattern() {
        SymbolTable symbols = parseAst(
            "module scotch.test",
            "left infix 6 (==), (/=)",
            "prefix 4 not",
            "x == y = not (x /= y)"
        );
        assertThat(symbols.getDefinition(valueRef("scotch.test", "==")), is(
            value(fromString("scotch.test.(==)"), t(8), patterns(
                pattern(asList(capture("x", t(0)), capture("y", t(2))), apply(
                    id("scotch.test.not", t(4)),
                    apply(
                        apply(id("scotch.test.(/=)", t(6)), id("x", t(5)), t(9)),
                        id("y", t(7)),
                        t(10)
                    ),
                    t(11)
                ))
            ))
        ));
    }
}
