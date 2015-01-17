package scotch.compiler.parser;

import static scotch.util.StringUtil.quote;
import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import scotch.compiler.error.SyntaxError;
import scotch.compiler.text.SourceRange;

public class ParseError extends SyntaxError {

    public static SyntaxError parseError(String description, SourceRange location) {
        return new ParseError(description, location);
    }

    private final String      description;
    private final SourceRange sourceRange;

    ParseError(String description, SourceRange sourceRange) {
        this.description = description;
        this.sourceRange = sourceRange;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof ParseError) {
            ParseError other = (ParseError) o;
            return Objects.equals(description, other.description)
                && Objects.equals(sourceRange, other.sourceRange);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, sourceRange);
    }

    @Override
    public String prettyPrint() {
        return description + " " + sourceRange.prettyPrint();
    }

    @Override
    public String toString() {
        return stringify(this) + "(description=" + quote(description) + ")";
    }
}
