package scotch.compiler.syntax.value;

import static me.qmx.jitescript.util.CodegenUtils.ci;
import static me.qmx.jitescript.util.CodegenUtils.p;
import static scotch.symbol.type.Types.sum;

import me.qmx.jitescript.CodeBlock;
import scotch.compiler.intermediate.IntermediateGenerator;
import scotch.compiler.intermediate.IntermediateValue;
import scotch.symbol.type.Types;
import scotch.compiler.text.SourceLocation;

public class BoolLiteral extends LiteralValue<Boolean> {

    BoolLiteral(SourceLocation sourceLocation, boolean value) {
        super(sourceLocation, value, Types.sum("scotch.data.bool.Bool"));
    }

    @Override
    public IntermediateValue generateIntermediateCode(IntermediateGenerator state) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    protected CodeBlock loadValue() {
        return new CodeBlock().getstatic(p(Boolean.class), getValue() ? "TRUE" : "FALSE", ci(Boolean.class));
    }
}
