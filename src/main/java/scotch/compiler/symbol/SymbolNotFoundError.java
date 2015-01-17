package scotch.compiler.symbol;

import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import scotch.compiler.error.SyntaxError;
import scotch.compiler.text.SourceRange;

public class SymbolNotFoundError extends SyntaxError {

    public static SyntaxError symbolNotFound(Symbol symbol, SourceRange location) {
        return new SymbolNotFoundError(symbol, location);
    }

    private final Symbol      symbol;
    private final SourceRange sourceRange;

    public SymbolNotFoundError(Symbol symbol, SourceRange sourceRange) {
        this.symbol = symbol;
        this.sourceRange = sourceRange;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof scotch.compiler.symbol.SymbolNotFoundError) {
            scotch.compiler.symbol.SymbolNotFoundError other = (scotch.compiler.symbol.SymbolNotFoundError) o;
            return Objects.equals(symbol, other.symbol)
                && Objects.equals(sourceRange, other.sourceRange);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, sourceRange);
    }

    @Override
    public String prettyPrint() {
        return "Symbol not found: " + symbol.quote() + " " + sourceRange.prettyPrint();
    }

    @Override
    public String toString() {
        return stringify(this) + "(symbol=" + symbol + ")";
    }
}
