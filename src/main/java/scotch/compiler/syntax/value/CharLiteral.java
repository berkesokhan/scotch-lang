package scotch.compiler.syntax.value;

import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static scotch.compiler.symbol.Type.sum;

import me.qmx.jitescript.CodeBlock;
import scotch.compiler.text.SourceRange;

public class CharLiteral extends LiteralValue<Character> {

    CharLiteral(SourceRange sourceRange, char value) {
        super(sourceRange, value, sum("scotch.data.char.Char"));
    }

    @Override
    protected CodeBlock loadValue() {
        return new CodeBlock() {{
            ldc(getValue());
            invokestatic(p(Character.class), "valueOf", sig(Character.class, char.class));
        }};
    }
}
