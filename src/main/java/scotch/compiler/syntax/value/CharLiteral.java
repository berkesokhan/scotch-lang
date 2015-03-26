package scotch.compiler.syntax.value;

import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static scotch.symbol.type.Types.sum;

import me.qmx.jitescript.CodeBlock;
import scotch.compiler.intermediate.IntermediateGenerator;
import scotch.compiler.intermediate.IntermediateValue;
import scotch.symbol.type.Types;
import scotch.compiler.text.SourceLocation;

public class CharLiteral extends LiteralValue<Character> {

    CharLiteral(SourceLocation sourceLocation, char value) {
        super(sourceLocation, value, Types.sum("scotch.data.char.Char"));
    }

    @Override
    public IntermediateValue generateIntermediateCode(IntermediateGenerator state) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    protected CodeBlock loadValue() {
        return new CodeBlock() {{
            ldc(getValue());
            invokestatic(p(Character.class), "valueOf", sig(Character.class, char.class));
        }};
    }
}
