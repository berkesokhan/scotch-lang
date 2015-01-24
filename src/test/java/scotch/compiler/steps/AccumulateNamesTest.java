package scotch.compiler.steps;

import static scotch.compiler.syntax.reference.DefinitionReference.moduleRef;
import static scotch.compiler.util.TestUtil.scopeRef;

import java.util.function.Function;
import org.junit.Test;
import scotch.compiler.*;
import scotch.compiler.Compiler;
import scotch.compiler.syntax.StubResolver;
import scotch.compiler.syntax.definition.DefinitionGraph;

public class AccumulateNamesTest extends ParserTest {

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
        shouldBeDefined(scopeRef("scotch.test.(fn#0#0)"), "a");
        shouldBeDefined(scopeRef("scotch.test.(fn#0#0)"), "b");
    }

    @Test
    public void shouldAliasLetDeclarations() {
        parse(
            "module scotch.test",
            "main = let",
            "    f x = a x",
            "    a g = g + g",
            "  f 2"
        );
    }

    @Override
    protected void initResolver(StubResolver resolver) {
        // intentionally empty
    }

    @Override
    protected Function<scotch.compiler.Compiler, DefinitionGraph> parse() {
        return Compiler::accumulateNames;
    }

    @Override
    protected void setUp() {
        // intentionally empty
    }
}
