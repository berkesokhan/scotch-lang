package scotch.compiler.analyzer;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static scotch.compiler.ast.Definition.signature;
import static scotch.compiler.ast.Definition.value;
import static scotch.compiler.ast.DefinitionReference.signatureRef;
import static scotch.compiler.ast.DefinitionReference.valueRef;
import static scotch.compiler.ast.PatternMatch.capture;
import static scotch.compiler.ast.PatternMatcher.pattern;
import static scotch.compiler.ast.Value.apply;
import static scotch.compiler.ast.Value.id;
import static scotch.compiler.ast.Value.literal;
import static scotch.compiler.ast.Value.patterns;
import static scotch.compiler.util.TestUtil.analyzeReferences;
import static scotch.lang.Type.fn;
import static scotch.lang.Type.intType;
import static scotch.lang.Type.lookup;
import static scotch.lang.Type.t;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import scotch.compiler.ast.SymbolTable;
import scotch.lang.Type;

public class NameQualifierTest {

    @Rule
    public final ExpectedException exception = none();
    private SetResolver resolver;

    @Before
    public void setUp() {
        resolver = new SetResolver()
            .addName("scotch.data.int.Int")
            .addName("scotch.data.num.(+)")
            .addName("scotch.data.bool.not")
            .addName("scotch.data.sequence.seq");
    }

    @Test
    public void shouldQualifyForwardReferences() {
        SymbolTable symbols = analyzeReferences(
            resolver,
            "module scotch.test",
            "import scotch.data.bool",
            "prefix 4 not",
            "left infix 5 (==), (/=)",
            "x == y = not x /= y",
            "x /= y = not x == y"
        );
        assertThat(symbols.getDefinition(valueRef("scotch.test", "==")), equalTo(
            value("scotch.test.(==)", t(1), patterns(
                pattern(asList(capture("x", t(0)), capture("y", t(2))), apply(
                    id("scotch.data.bool.not", t(3)),
                    apply(
                        apply(id("scotch.test.(/=)", t(4)), id("x", t(0)), t(5)),
                        id("y", t(2)),
                        t(6)
                    ),
                    t(7)
                ))
            ))
        ));
    }

    @Test
    public void shouldQualifyImportedReferences() {
        SymbolTable symbols = analyzeReferences(
            resolver,
            "module scotch.test",
            "import scotch.data.sequence",
            "srsly = seq"
        );
        assertThat(symbols.getDefinition(valueRef("scotch.test", "srsly")), equalTo(
            value("scotch.test.srsly", t(0), id("scotch.data.sequence.seq", t(1)))
        ));
    }

    @Test
    public void shouldQualifyNames() {
        Type fnType = fn(lookup("scotch.data.int.Int"), lookup("scotch.data.int.Int"));
        SymbolTable symbols = analyzeReferences(
            resolver,
            "module scotch.test",
            "import scotch.data.int",
            "import scotch.data.num",
            "fn :: Int -> Int",
            "fn n = n + 1"
        );
        assertThat(symbols.getDefinition(signatureRef("scotch.test", "fn")), equalTo(
            signature("scotch.test.fn", fnType)
        ));
        assertThat(symbols.getDefinition(valueRef("scotch.test", "fn")), equalTo(
            value("scotch.test.fn", fnType, patterns(
                pattern(asList(capture("n", t(1))), apply(apply(id("scotch.data.num.(+)", t(3)), id("n", t(1)), t(4)), literal(1, intType), t(5)))
            ))
        ));
    }

    @Test
    public void shouldThrow_whenUnableToQualifyReference() {
        exception.expect(SymbolNotFoundException.class);
        exception.expectMessage("Reference to undefined symbol 'undef'");
        analyzeReferences(
            resolver,
            "module scotch.test",
            "delete x = undef x"
        );
    }
}
