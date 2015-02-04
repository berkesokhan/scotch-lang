package scotch.compiler.syntax.value;

import static me.qmx.jitescript.util.CodegenUtils.ci;
import static me.qmx.jitescript.util.CodegenUtils.p;
import static scotch.compiler.symbol.type.Types.sum;

import me.qmx.jitescript.CodeBlock;
import scotch.compiler.symbol.type.Types;
import scotch.compiler.text.SourceRange;

public class BoolLiteral extends LiteralValue<Boolean> {

    BoolLiteral(SourceRange sourceRange, boolean value) {
        super(sourceRange, value, Types.sum("scotch.data.bool.Bool"));
    }

    @Override
    protected CodeBlock loadValue() {
        return new CodeBlock().getstatic(p(Boolean.class), getValue() ? "TRUE" : "FALSE", ci(Boolean.class));
    }
}
