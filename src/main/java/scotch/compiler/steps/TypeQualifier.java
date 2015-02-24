package scotch.compiler.steps;

import java.util.Optional;
import scotch.compiler.error.SyntaxError;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.SymbolNotFoundError;
import scotch.compiler.text.SourceRange;

public class TypeQualifier implements NameQualifier {

    private final TypeChecker typeChecker;

    public TypeQualifier(TypeChecker typeChecker) {
        this.typeChecker = typeChecker;
    }

    @Override
    public void error(SyntaxError error) {
        typeChecker.error(error);
    }

    @Override
    public Optional<Symbol> qualify(Symbol symbol) {
        return typeChecker.scope().qualify(symbol);
    }

    @Override
    public void symbolNotFound(Symbol symbol, SourceRange sourceRange) {
        error(SymbolNotFoundError.symbolNotFound(symbol, sourceRange));
    }
}
