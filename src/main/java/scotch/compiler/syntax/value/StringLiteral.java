package scotch.compiler.syntax.value;

import static scotch.symbol.type.Types.sum;

import me.qmx.jitescript.CodeBlock;
import scotch.symbol.type.Types;
import scotch.compiler.text.SourceLocation;

public class StringLiteral extends LiteralValue<String> {

    StringLiteral(SourceLocation sourceLocation, String value) {
        super(sourceLocation, value, Types.sum("scotch.data.string.String"));
    }

    @Override
    protected CodeBlock loadValue() {
        return new CodeBlock().ldc(getValue());
    }
}
