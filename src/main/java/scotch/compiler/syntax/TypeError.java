package scotch.compiler.syntax;

import static lombok.AccessLevel.PRIVATE;
import static scotch.compiler.text.TextUtil.repeat;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import scotch.compiler.error.SyntaxError;
import scotch.symbol.type.Unification;
import scotch.compiler.text.SourceLocation;

@AllArgsConstructor(access = PRIVATE)
@EqualsAndHashCode(callSuper = false)
@ToString
public class TypeError extends SyntaxError {

    public static SyntaxError typeError(Unification unification, SourceLocation location) {
        return new TypeError(unification, location);
    }

    @NonNull private final Unification    unification;
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
        return unification.prettyPrint();
    }
}
