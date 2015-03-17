package scotch.compiler.syntax.value;

import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static scotch.symbol.type.Types.sum;

import me.qmx.jitescript.CodeBlock;
import scotch.symbol.type.Types;
import scotch.compiler.text.SourceRange;

public class DoubleLiteral extends LiteralValue<Double> {

    DoubleLiteral(SourceRange sourceRange, double value) {
        super(sourceRange, value, Types.sum("scotch.data.double.Double"));
    }

    @Override
    protected CodeBlock loadValue() {
        return new CodeBlock() {{
            ldc(getValue());
            invokestatic(p(Double.class), "valueOf", sig(Double.class, double.class));
        }};
    }
}
