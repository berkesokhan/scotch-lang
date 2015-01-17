package scotch.compiler;

import static java.util.Arrays.asList;
import static scotch.compiler.symbol.Type.t;
import static scotch.compiler.syntax.value.Value.apply;
import static scotch.compiler.util.TestUtil.arg;
import static scotch.compiler.util.TestUtil.capture;
import static scotch.compiler.util.TestUtil.fn;
import static scotch.compiler.util.TestUtil.id;
import static scotch.compiler.util.TestUtil.pattern;
import static scotch.compiler.util.TestUtil.patterns;

import java.util.function.Function;
import org.junit.Test;
import scotch.compiler.syntax.StubResolver;
import scotch.compiler.syntax.definition.DefinitionGraph;

public class QualifyNamesTest extends ParserTest {

    @Test
    public void shouldQualifyNames() {
        parse(
            "module scotch.test",
            "fn1 a b = fn2 a b",
            "fn2 a b = a b"
        );
        shouldNotHaveErrors();
        shouldHaveValue("scotch.test.fn1", fn(
            "scotch.test.($2)",
            asList(arg("$0", t(11)), arg("$1", t(12))),
            patterns(t(13), pattern(
                "scotch.test.($0)",
                asList(capture("$0", "a", t(1)), capture("$1", "b", t(2))),
                apply(
                    apply(id("scotch.test.fn2", t(3)), id("a", t(4)), t(15)),
                    id("b", t(5)),
                    t(16)
                )
            ))
        ));
    }

    @Override
    protected void initResolver(StubResolver resolver) {
        // intentionally empty
    }

    @Override
    protected Function<Compiler, DefinitionGraph> parse() {
        return Compiler::qualifyNames;
    }

    @Override
    protected void setUp() {
        // intentionally empty
    }
}
