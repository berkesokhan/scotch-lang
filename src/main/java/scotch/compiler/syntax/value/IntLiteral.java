package scotch.compiler.syntax.value;

import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static scotch.compiler.symbol.Type.sum;

import me.qmx.jitescript.CodeBlock;
import scotch.compiler.text.SourceRange;

public class IntLiteral extends LiteralValue<Integer> {

    IntLiteral(SourceRange sourceRange, int value) {
        super(sourceRange, value, sum("scotch.data.int.Int"));
    }

    @Override
    protected CodeBlock loadValue() {
        return new CodeBlock() {{
            ldc(getValue());
            invokestatic(p(Integer.class), "valueOf", sig(Integer.class, int.class));
        }};
    }
}
