package scotch.compiler.syntax.value;

import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;

import me.qmx.jitescript.CodeBlock;
import scotch.symbol.type.Types;
import scotch.compiler.text.SourceLocation;

public class IntLiteral extends LiteralValue<Integer> {

    IntLiteral(SourceLocation sourceLocation, int value) {
        super(sourceLocation, value, Types.sum("scotch.data.int.Int"));
    }

    @Override
    protected CodeBlock loadValue() {
        return new CodeBlock() {{
            ldc(getValue());
            invokestatic(p(Integer.class), "valueOf", sig(Integer.class, int.class));
        }};
    }
}
