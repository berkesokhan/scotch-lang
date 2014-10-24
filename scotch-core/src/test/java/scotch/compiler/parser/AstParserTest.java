package scotch.compiler.parser;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.ast.Definition.value;
import static scotch.compiler.ast.DefinitionReference.valueRef;
import static scotch.compiler.ast.PatternMatch.capture;
import static scotch.compiler.ast.PatternMatcher.pattern;
import static scotch.compiler.ast.Value.apply;
import static scotch.compiler.ast.Value.id;
import static scotch.compiler.ast.Value.patterns;
import static scotch.compiler.util.TestUtil.parseAst;
import static scotch.lang.Symbol.fromString;
import static scotch.lang.Type.t;

import org.junit.Test;
import scotch.compiler.ast.SymbolTable;

public class AstParserTest {

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
                    id("not", t(4)),
                    apply(
                        apply(id("/=", t(6)), id("x", t(5)), t(9)),
                        id("y", t(7)),
                        t(10)
                    ),
                    t(11)
                ))
            ))
        ));
    }
}
