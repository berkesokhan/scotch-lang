package scotch.compiler.syntax.value;

import static scotch.compiler.symbol.type.Type.sum;

import me.qmx.jitescript.CodeBlock;
import scotch.compiler.text.SourceRange;

public class StringLiteral extends LiteralValue<String> {

    StringLiteral(SourceRange sourceRange, String value) {
        super(sourceRange, value, sum("scotch.data.string.String"));
    }

    @Override
    protected CodeBlock loadValue() {
        return new CodeBlock().ldc(getValue());
    }
}
