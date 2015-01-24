package scotch.compiler.syntax.value;

import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static scotch.compiler.symbol.type.Type.sum;

import me.qmx.jitescript.CodeBlock;
import scotch.compiler.text.SourceRange;

public class DoubleLiteral extends LiteralValue<Double> {

    DoubleLiteral(SourceRange sourceRange, double value) {
        super(sourceRange, value, sum("scotch.data.double.Double"));
    }

    @Override
    protected CodeBlock loadValue() {
        return new CodeBlock() {{
            ldc(getValue());
            invokestatic(p(Double.class), "valueOf", sig(Double.class, double.class));
        }};
    }
}
