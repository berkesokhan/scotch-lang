package scotch.compiler.syntax.value;

import static lombok.AccessLevel.PRIVATE;
import static scotch.compiler.text.TextUtil.repeat;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import scotch.compiler.error.SyntaxError;
import scotch.symbol.Symbol;
import scotch.compiler.text.SourceRange;

@AllArgsConstructor(access = PRIVATE)
@EqualsAndHashCode(callSuper = false)
@ToString
public class NoBindingError extends SyntaxError {

    public static NoBindingError noBinding(Symbol symbol, SourceRange sourceRange) {
        return new NoBindingError(symbol, sourceRange);
    }

    @NonNull private final Symbol      symbol;
    @NonNull private final SourceRange sourceRange;

    @Override
    public String prettyPrint() {
        return prettyPrint_() + " " + sourceRange.prettyPrint();
    }

    @Override
    public String report(String indent, int indentLevel) {
        return sourceRange.report(indent, indentLevel) + "\n"
            + repeat(indent, indentLevel + 1) + prettyPrint_();
    }

    private String prettyPrint_() {
        return "No binding found for method " + symbol;
    }
}
