package scotch.compiler.steps;

import static scotch.compiler.syntax.reference.DefinitionReference.moduleRef;
import static scotch.compiler.util.TestUtil.scopeRef;

import java.util.function.Function;
import org.junit.Test;
import scotch.compiler.Compiler;
import scotch.compiler.IsolatedCompilerTest;
import scotch.compiler.syntax.definition.DefinitionGraph;

public class AccumulateNamesTest extends IsolatedCompilerTest {

    @Test
    public void shouldDefineFirstLevelValues() {
        compile(
            "module scotch.test",
            "fn a b = a b"
        );
        shouldNotHaveErrors();
        shouldBeDefined(moduleRef("scotch.test"), "scotch.test.fn");
    }

    @Test
    public void shouldDefinePatternArguments() {
        compile(
            "module scotch.test",
            "fn a b = a b"
        );
        shouldNotHaveErrors();
        shouldBeDefined(scopeRef("scotch.test.(fn#0#0)"), "a");
        shouldBeDefined(scopeRef("scotch.test.(fn#0#0)"), "b");
    }

    @Test
    public void shouldAliasLetDeclarations() {
        compile(
            "module scotch.test",
            "main = let",
            "    f x = a x",
            "    a g = g + g",
            "  f 2"
        );
    }

    @Override
    protected Function<scotch.compiler.Compiler, DefinitionGraph> compile() {
        return Compiler::accumulateNames;
    }
}
