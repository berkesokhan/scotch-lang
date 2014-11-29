package scotch.compiler.syntax;

public interface SymbolResolver {

    boolean isDefined(Symbol symbol);

    SymbolEntry getEntry(Symbol symbol);
}
