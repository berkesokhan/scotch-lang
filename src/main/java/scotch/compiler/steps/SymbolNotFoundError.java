package scotch.compiler.steps;

import static lombok.AccessLevel.PRIVATE;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import scotch.compiler.error.SyntaxError;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.text.SourceRange;

@AllArgsConstructor(access = PRIVATE)
@EqualsAndHashCode(callSuper = false)
@ToString
public class SymbolNotFoundError extends SyntaxError {

    public static SyntaxError symbolNotFound(Symbol symbol, SourceRange location) {
        return new SymbolNotFoundError(symbol, location);
    }

    @NonNull private final Symbol      symbol;
    @NonNull private final SourceRange sourceRange;

    @Override
    public String prettyPrint() {
        return "Symbol not found: " + symbol.quote() + " " + sourceRange.prettyPrint();
    }
}
