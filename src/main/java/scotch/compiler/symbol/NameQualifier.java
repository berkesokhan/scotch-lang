package scotch.compiler.symbol;

import java.util.Optional;
import scotch.compiler.text.SourceRange;

public interface NameQualifier {

    void symbolNotFound(Symbol symbol, SourceRange sourceRange);

    Optional<Symbol> qualify(Symbol symbol);
}
