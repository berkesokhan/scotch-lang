package scotch.compiler.syntax.value;

import static lombok.AccessLevel.PRIVATE;
import static scotch.compiler.text.TextUtil.repeat;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import scotch.compiler.error.SyntaxError;
import scotch.symbol.Symbol;
import scotch.compiler.text.SourceLocation;

@AllArgsConstructor(access = PRIVATE)
@EqualsAndHashCode(callSuper = false)
@ToString
public class NoBindingError extends SyntaxError {

    public static NoBindingError noBinding(Symbol symbol, SourceLocation sourceLocation) {
        return new NoBindingError(symbol, sourceLocation);
    }

    @NonNull private final Symbol         symbol;
    @NonNull private final SourceLocation sourceLocation;

    @Override
    public String prettyPrint() {
        return prettyPrint_() + " " + sourceLocation.prettyPrint();
    }

    @Override
    public String report(String indent, int indentLevel) {
        return sourceLocation.report(indent, indentLevel) + "\n"
            + repeat(indent, indentLevel + 1) + prettyPrint_();
    }

    private String prettyPrint_() {
        return "No binding found for method " + symbol;
    }
}
