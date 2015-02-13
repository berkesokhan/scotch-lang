package scotch.compiler.parser;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import scotch.compiler.error.SyntaxError;
import scotch.compiler.text.SourceRange;

@EqualsAndHashCode
@ToString
public class ParseError extends SyntaxError {

    public static SyntaxError parseError(String description, SourceRange location) {
        return new ParseError(description, location);
    }

    @NonNull private final String      description;
    @NonNull private final SourceRange sourceRange;

    ParseError(String description, SourceRange sourceRange) {
        this.description = description;
        this.sourceRange = sourceRange;
    }

    @Override
    public String prettyPrint() {
        return description + " " + sourceRange.prettyPrint();
    }
}
