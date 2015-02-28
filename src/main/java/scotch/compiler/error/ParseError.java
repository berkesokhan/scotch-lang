package scotch.compiler.error;

import static lombok.AccessLevel.PRIVATE;
import static scotch.compiler.text.TextUtil.repeat;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import scotch.compiler.text.SourceRange;

@AllArgsConstructor(access = PRIVATE)
@EqualsAndHashCode(callSuper = false)
@ToString
public class ParseError extends SyntaxError {

    public static SyntaxError parseError(String description, SourceRange location) {
        return new ParseError(description, location);
    }

    @NonNull private final String      description;
    @NonNull private final SourceRange sourceRange;

    @Override
    public String prettyPrint() {
        return description + " " + sourceRange.prettyPrint();
    }

    @Override
    public String report(String indent, int indentLevel) {
        return sourceRange.report(indent, indentLevel) + "\n"
            + repeat(indent, indentLevel + 1) + description;
    }
}
