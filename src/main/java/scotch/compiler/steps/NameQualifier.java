package scotch.compiler.steps;

import java.util.Optional;
import scotch.compiler.error.SyntaxError;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.text.SourceRange;

public interface NameQualifier {

    void error(SyntaxError error);

    Optional<Symbol> qualify(Symbol symbol);

    void symbolNotFound(Symbol symbol, SourceRange sourceRange);
}
