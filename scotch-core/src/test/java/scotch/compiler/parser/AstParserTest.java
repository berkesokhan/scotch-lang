package scotch.compiler.parser;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static scotch.compiler.ast.Definition.value;
import static scotch.compiler.ast.DefinitionReference.valueRef;
import static scotch.compiler.ast.PatternMatch.capture;
import static scotch.compiler.ast.PatternMatch.equal;
import static scotch.compiler.ast.PatternMatcher.pattern;
import static scotch.compiler.ast.Symbol.qualified;
import static scotch.compiler.ast.Symbol.unqualified;
import static scotch.compiler.ast.Type.t;
import static scotch.compiler.ast.Value.apply;
import static scotch.compiler.ast.Value.id;
import static scotch.compiler.ast.Value.literal;
import static scotch.compiler.ast.Value.patterns;
import static scotch.compiler.util.TestUtil.parseAst;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import scotch.compiler.ast.SymbolResolver;
import scotch.compiler.ast.SymbolTable;

@RunWith(MockitoJUnitRunner.class)
public class AstParserTest {

    @Mock
    private SymbolResolver resolver;

    @Test
    public void shouldShufflePattern() {
        when(resolver.isDefined(qualified("scotch.data.bool", "not"))).thenReturn(true);
        SymbolTable symbols = parseAst(
            resolver,
            "module scotch.test",
            "import scotch.data.bool",
            "left infix 6 (==), (/=)",
            "x == y = not (x /= y)"
        );
        assertThat(symbols.getDefinition(valueRef("scotch.test", "==")), is(
            value("scotch.test.(==)", t(8), patterns(t(9),
                pattern(qualified("scotch.test", "pattern#3"), asList(capture("x", t(0)), capture("y", t(2))), apply(
                    id("scotch.data.bool.not", t(4)),
                    apply(
                        apply(id("scotch.test.(/=)", t(6)), id("x", t(5)), t(10)),
                        id("y", t(7)),
                        t(11)
                    ),
                    t(12)
                ))
            ))
        ));
    }

    @Test
    public void shouldConsolidatePatternsIntoSingleValue() {
        SymbolTable symbols = parseAst(
            resolver,
            "module scotch.test",
            "left infix 8 (+), (-)",
            "fib 0 = 0",
            "fib 1 = 1",
            "fib n = fib (n - 1) + fib (n - 2)"
        );
        assertThat(symbols.getDefinition(valueRef("scotch.test", "fib")), is(
            value("scotch.test.fib", t(20), patterns(t(20),
                pattern(qualified("scotch.test", "pattern#2"), asList(equal(literal(0, t(1)))), literal(0, t(3))),
                pattern(qualified("scotch.test", "pattern#6"), asList(equal(literal(1, t(5)))), literal(1, t(7))),
                pattern(qualified("scotch.test", "pattern#10"), asList(capture(unqualified("n"), t(9))), apply(
                    apply(
                        id("scotch.test.(+)", t(15)),
                        apply(
                            id("scotch.test.fib", t(11)),
                            apply(
                                apply(id("scotch.test.(-)", t(13)), id("n", t(12)), t(22)),
                                literal(1, t(14)),
                                t(23)
                            ),
                            t(24)
                        ),
                        t(28)
                    ),
                    apply(
                        id("scotch.test.fib", t(16)),
                        apply(
                            apply(id("scotch.test.(-)", t(18)), id("n", t(17)), t(25)),
                            literal(2, t(19)),
                            t(26)
                        ),
                        t(27)
                    ),
                    t(29)
                ))
            ))
        ));
    }
}
