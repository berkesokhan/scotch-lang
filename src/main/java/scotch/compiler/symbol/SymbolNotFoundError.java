package scotch.compiler.symbol;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import scotch.compiler.error.SyntaxError;
import scotch.compiler.text.SourceRange;

@EqualsAndHashCode(callSuper = false)
@ToString
public class SymbolNotFoundError extends SyntaxError {

    public static SyntaxError symbolNotFound(Symbol symbol, SourceRange location) {
        return new SymbolNotFoundError(symbol, location);
    }

    @NonNull private final Symbol      symbol;
    @NonNull private final SourceRange sourceRange;

    public SymbolNotFoundError(Symbol symbol, SourceRange sourceRange) {
        this.symbol = symbol;
        this.sourceRange = sourceRange;
    }

    @Override
    public String prettyPrint() {
        return "Symbol not found: " + symbol.quote() + " " + sourceRange.prettyPrint();
    }
}
