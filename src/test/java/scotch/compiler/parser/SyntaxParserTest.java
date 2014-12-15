package scotch.compiler.parser;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.symbol.Symbol.qualified;
import static scotch.compiler.symbol.SymbolEntry.immutableEntry;
import static scotch.compiler.symbol.Type.fn;
import static scotch.compiler.symbol.Type.sum;
import static scotch.compiler.symbol.Type.t;
import static scotch.compiler.symbol.Type.var;
import static scotch.compiler.syntax.DefinitionReference.classRef;
import static scotch.compiler.syntax.DefinitionReference.signatureRef;
import static scotch.compiler.syntax.DefinitionReference.valueRef;
import static scotch.compiler.syntax.Value.apply;
import static scotch.compiler.util.TestUtil.capture;
import static scotch.compiler.util.TestUtil.classDef;
import static scotch.compiler.util.TestUtil.equal;
import static scotch.compiler.util.TestUtil.id;
import static scotch.compiler.util.TestUtil.intType;
import static scotch.compiler.util.TestUtil.literal;
import static scotch.compiler.util.TestUtil.parseSyntax;
import static scotch.compiler.util.TestUtil.pattern;
import static scotch.compiler.util.TestUtil.patterns;
import static scotch.compiler.util.TestUtil.value;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import scotch.compiler.syntax.DefinitionGraph;
import scotch.compiler.syntax.StubResolver;

public class SyntaxParserTest {

    private StubResolver resolver;

    @Before
    public void setUp() {
        resolver = new StubResolver()
            .define(immutableEntry(qualified("scotch.data.int", "Int")).withType(intType()).build());
    }

    @Test
    public void shouldShufflePattern() {
        resolver.define(immutableEntry(qualified("scotch.data.bool", "not")).build());
        DefinitionGraph graph = parseSyntax(
            resolver,
            "module scotch.test",
            "import scotch.data.bool",
            "left infix 6 (==), (/=)",
            "x == y = not (x /= y)"
        );
        assertThat(graph.getDefinition(valueRef("scotch.test", "==")).get(), is(
            value("scotch.test.(==)", t(8), patterns(t(9),
                pattern("scotch.test.(pattern#3)", asList(capture("x", t(0)), capture("y", t(2))), apply(
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
        DefinitionGraph graph = parseSyntax(
            resolver,
            "module scotch.test",
            "left infix 8 (+), (-)",
            "fib 0 = 0",
            "fib 1 = 1",
            "fib n = fib (n - 1) + fib (n - 2)"
        );
        assertThat(graph.getDefinition(valueRef("scotch.test", "fib")).get(), is(
            value("scotch.test.fib", t(14), patterns(t(15),
                pattern("scotch.test.(pattern#1)", asList(equal(literal(0))), literal(0)),
                pattern("scotch.test.(pattern#3)", asList(equal(literal(1))), literal(1)),
                pattern("scotch.test.(pattern#6)", asList(capture("n", t(5))), apply(
                    apply(
                        id("scotch.test.(+)", t(10)),
                        apply(
                            id("scotch.test.fib", t(7)),
                            apply(
                                apply(id("scotch.test.(-)", t(9)), id("n", t(8)), t(16)),
                                literal(1),
                                t(17)
                            ),
                            t(18)
                        ),
                        t(22)
                    ),
                    apply(
                        id("scotch.test.fib", t(11)),
                        apply(
                            apply(id("scotch.test.(-)", t(13)), id("n", t(12)), t(19)),
                            literal(2),
                            t(20)
                        ),
                        t(21)
                    ),
                    t(23)
                ))
            ))
        ));
    }

    @Test
    public void shouldQualifyTypeNames() {
        DefinitionGraph graph = parseSyntax(
            resolver,
            "module scotch.test",
            "import scotch.data.int",
            "fn :: Int -> Int",
            "fn n = n"
        );
        assertThat(graph.getValue(valueRef("scotch.test", "fn")).get(), is(fn(intType(), intType())));
    }

    @Ignore
    @Test
    public void shouldParseTypeClass() {
        DefinitionGraph graph = parseSyntax(
            resolver,
            "module scotch.test",
            "import scotch.data.bool",
            "class Eq a where",
            "    (==), (/=) :: a -> a -> Bool",
            "    x == y = not $ x /= y",
            "    x /= y = not $ x == y"
        );
        assertThat(graph.getDefinition(classRef("scotch.test", "Eq")), is(
            classDef("scotch.test.Eq", asList(var("a")), asList(
                signatureRef("scotch.test", "=="),
                signatureRef("scotch.test", "/="),
                valueRef("scotch.test", "=="),
                valueRef("scotch.test", "/=")
            ))
        ));
        assertThat(graph.getDefinition(signatureRef("scotch.test", "==")), is(fn(var("a", asList("Eq")), fn(var("a", asList("Eq")), sum("Bool")))));
    }
}
