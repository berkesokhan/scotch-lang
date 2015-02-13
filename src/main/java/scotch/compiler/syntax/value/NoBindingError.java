package scotch.compiler.syntax.value;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import scotch.compiler.error.SyntaxError;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.text.SourceRange;

@EqualsAndHashCode
@ToString
public class NoBindingError extends SyntaxError {

    public static NoBindingError noBinding(Symbol symbol, SourceRange sourceRange) {
        return new NoBindingError(symbol, sourceRange);
    }

    @NonNull private final Symbol      symbol;
    @NonNull private final SourceRange sourceRange;

    private NoBindingError(Symbol symbol, SourceRange sourceRange) {
        this.symbol = symbol;
        this.sourceRange = sourceRange;
    }

    @Override
    public String prettyPrint() {
        return "No binding found for method " + symbol + " " + sourceRange.prettyPrint();
    }
}
