package scotch.compiler.parser;

import static scotch.compiler.syntax.DefinitionReference.moduleRef;
import static scotch.compiler.util.TestUtil.scopeRef;

import java.util.function.Function;
import org.junit.Test;
import scotch.compiler.Compiler;
import scotch.compiler.syntax.DefinitionGraph;
import scotch.compiler.syntax.StubResolver;

public class NameAccumulatorTest extends ParserTest {

    @Test
    public void shouldDefineFirstLevelValues() {
        parse(
            "module scotch.test",
            "fn a b = a b"
        );
        shouldNotHaveErrors();
        shouldBeDefined(moduleRef("scotch.test"), "scotch.test.fn");
    }

    @Test
    public void shouldDefinePatternArguments() {
        parse(
            "module scotch.test",
            "fn a b = a b"
        );
        shouldNotHaveErrors();
        shouldBeDefined(scopeRef("scotch.test.($0)"), "a");
        shouldBeDefined(scopeRef("scotch.test.($0)"), "b");
    }

    @Override
    protected void initResolver(StubResolver resolver) {
        // intentionally empty
    }

    @Override
    protected Function<Compiler, DefinitionGraph> parse() {
        return Compiler::accumulateNames;
    }

    @Override
    protected void setUp() {
        // intentionally empty
    }
}
