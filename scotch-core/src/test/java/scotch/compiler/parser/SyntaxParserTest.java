package scotch.compiler.parser;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.syntax.Definition.classDef;
import static scotch.compiler.syntax.Definition.value;
import static scotch.compiler.syntax.DefinitionReference.classRef;
import static scotch.compiler.syntax.DefinitionReference.signatureRef;
import static scotch.compiler.syntax.DefinitionReference.valueRef;
import static scotch.compiler.syntax.PatternMatch.capture;
import static scotch.compiler.syntax.PatternMatch.equal;
import static scotch.compiler.syntax.PatternMatcher.pattern;
import static scotch.compiler.syntax.Symbol.qualified;
import static scotch.compiler.syntax.Symbol.unqualified;
import static scotch.compiler.syntax.SymbolEntry.immutableEntry;
import static scotch.compiler.syntax.Type.fn;
import static scotch.compiler.syntax.Type.sum;
import static scotch.compiler.syntax.Type.t;
import static scotch.compiler.syntax.Type.var;
import static scotch.compiler.syntax.Value.apply;
import static scotch.compiler.syntax.Value.id;
import static scotch.compiler.syntax.Value.literal;
import static scotch.compiler.syntax.Value.patterns;
import static scotch.compiler.util.TestUtil.parseSyntax;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import scotch.compiler.syntax.StubResolver;
import scotch.compiler.syntax.SymbolTable;

public class SyntaxParserTest {

    private StubResolver resolver;

    @Before
    public void setUp() {
        resolver = new StubResolver()
            .define(immutableEntry(qualified("scotch.data.int", "Int")).withType(sum("scotch.data.int.Int")).build());
    }

    @Test
    public void shouldShufflePattern() {
        resolver.define(immutableEntry(qualified("scotch.data.bool", "not")).build());
        SymbolTable symbols = parseSyntax(
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
        SymbolTable symbols = parseSyntax(
            resolver,
            "module scotch.test",
            "left infix 8 (+), (-)",
            "fib 0 = 0",
            "fib 1 = 1",
            "fib n = fib (n - 1) + fib (n - 2)"
        );
        assertThat(symbols.getDefinition(valueRef("scotch.test", "fib")), is(
            value("scotch.test.fib", t(20), patterns(t(21),
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

    @Test
    public void shouldQualifyTypeNames() {
        SymbolTable symbols = parseSyntax(
            resolver,
            "module scotch.test",
            "import scotch.data.int",
            "fn :: Int -> Int",
            "fn n = n"
        );
        assertThat(symbols.getValue(valueRef("scotch.test", "fn")), is(fn(sum("scotch.data.int.Int"), sum("scotch.data.int.Int"))));
    }

    @Ignore
    @Test
    public void shouldParseTypeClass() {
        SymbolTable symbols = parseSyntax(
            resolver,
            "module scotch.test",
            "import scotch.data.bool",
            "class Eq a where",
            "    (==), (/=) :: a -> a -> Bool",
            "    x == y = not $ x /= y",
            "    x /= y = not $ x == y"
        );
        assertThat(symbols.getDefinition(classRef("scotch.test", "Eq")), is(
            classDef("scotch.test.Eq", asList(var("a")), asList(
                signatureRef("scotch.test", "=="),
                signatureRef("scotch.test", "/="),
                valueRef("scotch.test", "=="),
                valueRef("scotch.test", "/=")
            ))
        ));
        assertThat(symbols.getDefinition(signatureRef("scotch.test", "==")), is(fn(var("a", asList("Eq")), fn(var("a", asList("Eq")), sum("Bool")))));
    }
}
