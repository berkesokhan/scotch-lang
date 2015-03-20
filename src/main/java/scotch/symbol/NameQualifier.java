package scotch.symbol;

import java.util.Optional;
import scotch.compiler.error.SyntaxError;
import scotch.compiler.text.SourceLocation;

public interface NameQualifier {

    void error(SyntaxError error);

    Optional<Symbol> qualify(Symbol symbol);

    void symbolNotFound(Symbol symbol, SourceLocation sourceLocation);
}
