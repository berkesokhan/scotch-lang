package scotch.compiler.syntax;

import static lombok.AccessLevel.PRIVATE;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import scotch.compiler.error.SyntaxError;
import scotch.compiler.symbol.Unification;
import scotch.compiler.text.SourceRange;

@AllArgsConstructor(access = PRIVATE)
@EqualsAndHashCode
@ToString
public class TypeError extends SyntaxError {

    public static SyntaxError typeError(Unification unification, SourceRange location) {
        return new TypeError(unification, location);
    }

    @NonNull private final Unification unification;
    @NonNull private final          SourceRange sourceRange;

    @Override
    public String prettyPrint() {
        return unification.prettyPrint() + " " + sourceRange.prettyPrint();
    }
}
