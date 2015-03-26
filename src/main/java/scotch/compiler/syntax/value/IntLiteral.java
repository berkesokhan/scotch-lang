package scotch.compiler.syntax.value;

import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static scotch.compiler.intermediate.Intermediates.literal;

import me.qmx.jitescript.CodeBlock;
import scotch.compiler.intermediate.IntermediateGenerator;
import scotch.compiler.intermediate.IntermediateValue;
import scotch.compiler.text.SourceLocation;
import scotch.symbol.type.Types;

public class IntLiteral extends LiteralValue<Integer> {

    IntLiteral(SourceLocation sourceLocation, int value) {
        super(sourceLocation, value, Types.sum("scotch.data.int.Int"));
    }

    @Override
    public IntermediateValue generateIntermediateCode(IntermediateGenerator state) {
        return literal(value);
    }

    @Override
    protected CodeBlock loadValue() {
        return new CodeBlock() {{
            ldc(getValue());
            invokestatic(p(Integer.class), "valueOf", sig(Integer.class, int.class));
        }};
    }
}
