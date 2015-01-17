package scotch.compiler.syntax;

import java.util.Objects;
import scotch.compiler.error.SyntaxError;
import scotch.compiler.symbol.Unification;
import scotch.compiler.text.SourceRange;

public class TypeError extends SyntaxError {

    public static SyntaxError typeError(Unification unification, SourceRange location) {
        return new TypeError(unification, location);
    }

    private final Unification unification;
    private final SourceRange sourceRange;

    public TypeError(Unification unification, SourceRange sourceRange) {
        this.unification = unification;
        this.sourceRange = sourceRange;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof scotch.compiler.syntax.TypeError) {
            scotch.compiler.syntax.TypeError other = (scotch.compiler.syntax.TypeError) o;
            return Objects.equals(unification, other.unification)
                && Objects.equals(sourceRange, other.sourceRange);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(unification);
    }

    @Override
    public String prettyPrint() {
        return unification.prettyPrint() + " " + sourceRange.prettyPrint();
    }

    @Override
    public String toString() {
        return "TypeError(" + unification + ")";
    }
}
