package scotch.compiler.syntax.value;

import static scotch.symbol.type.Types.sum;

import me.qmx.jitescript.CodeBlock;
import scotch.symbol.type.Types;
import scotch.compiler.text.SourceRange;

public class StringLiteral extends LiteralValue<String> {

    StringLiteral(SourceRange sourceRange, String value) {
        super(sourceRange, value, Types.sum("scotch.data.string.String"));
    }

    @Override
    protected CodeBlock loadValue() {
        return new CodeBlock().ldc(getValue());
    }
}
