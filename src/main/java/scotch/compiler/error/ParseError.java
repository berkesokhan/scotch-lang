package scotch.compiler.error;

import static lombok.AccessLevel.PRIVATE;
import static scotch.compiler.text.TextUtil.repeat;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import scotch.compiler.text.SourceLocation;

@AllArgsConstructor(access = PRIVATE)
@EqualsAndHashCode(callSuper = false)
@ToString
public class ParseError extends SyntaxError {

    public static SyntaxError parseError(String description, SourceLocation location) {
        return new ParseError(description, location);
    }

    @NonNull private final String         description;
    @NonNull private final SourceLocation sourceLocation;

    @Override
    public String prettyPrint() {
        return description + " " + sourceLocation.prettyPrint();
    }

    @Override
    public String report(String indent, int indentLevel) {
        return sourceLocation.report(indent, indentLevel) + "\n"
            + repeat(indent, indentLevel + 1) + description;
    }
}
