package scotch.symbol;

import static me.qmx.jitescript.CodeBlock.ACC_STATIC;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import me.qmx.jitescript.CodeBlock;
import me.qmx.jitescript.JiteClass;

@AllArgsConstructor(staticName = "fieldSignature")
@EqualsAndHashCode(callSuper = false)
@ToString
public class FieldSignature {

    public static FieldSignature fieldSignature(String classSignature, int fieldAccess, String fieldName, String fieldSignature) {
        return new FieldSignature(classSignature, fieldAccess, fieldName, fieldSignature, Optional.empty());
    }

    private final String           classSignature;
    private final int              fieldAccess;
    private final String           fieldName;
    private final String           fieldSignature;
    private final Optional<Object> defaultValue;

    public void defineOn(JiteClass jiteClass) {
        jiteClass.defineField(fieldName, fieldAccess, fieldSignature, defaultValue.orElse(null));
    }

    public CodeBlock getValue() {
        return new CodeBlock() {{
            if (isStatic()) {
                getstatic(classSignature, fieldName, fieldSignature);
            } else {
                getfield(classSignature, fieldName, fieldSignature);
            }
        }};
    }

    public CodeBlock putValue() {
        return new CodeBlock() {{
            if (isStatic()) {
                putstatic(classSignature, fieldName, fieldSignature);
            } else {
                putfield(classSignature, fieldName, fieldSignature);
            }
        }};
    }

    private boolean isStatic() {
        return (fieldAccess & ACC_STATIC) == ACC_STATIC;
    }
}
