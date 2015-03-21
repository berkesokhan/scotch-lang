package scotch.compiler.steps;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.syntax.reference.DefinitionReference.moduleRef;
import static scotch.compiler.syntax.reference.DefinitionReference.operatorRef;
import static scotch.symbol.Operator.operator;
import static scotch.symbol.Symbol.symbol;
import static scotch.symbol.Value.Fixity.RIGHT_INFIX;

import java.util.function.Function;
import org.junit.Test;
import scotch.compiler.Compiler;
import scotch.compiler.IsolatedCompilerTest;
import scotch.compiler.syntax.definition.DefinitionGraph;
import scotch.compiler.syntax.definition.OperatorDefinition;
import scotch.symbol.Value.Fixity;

public class DefineOperatorsTest extends IsolatedCompilerTest {

    @Test
    public void shouldParseOperator() {
        compile(
            "module scotch.test",
            "right infix 0 (>>=), (>>)"
        );
        shouldNotHaveErrors();
        shouldBeDefined(moduleRef("scotch.test"), "scotch.test.(>>=)");
        shouldBeDefined(moduleRef("scotch.test"), "scotch.test.(>>)");
        shouldHaveOperator("scotch.test.(>>=)", RIGHT_INFIX, 0);
        shouldHaveOperator("scotch.test.(>>)", RIGHT_INFIX, 0);
    }

    private void shouldHaveOperator(String name, Fixity fixity, int precedence) {
        assertThat(((OperatorDefinition) graph.getDefinition(operatorRef(symbol(name))).get()).getOperator(), is(operator(fixity, precedence)));
    }

    protected Function<scotch.compiler.Compiler, DefinitionGraph> compile() {
        return Compiler::accumulateOperators;
    }
}
