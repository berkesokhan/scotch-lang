package scotch.compiler.symbol;

public class SymbolNotFoundException extends RuntimeException {

    public static SymbolNotFoundException symbolNotFound(Symbol symbol) {
        return new SymbolNotFoundException("Could not find symbol " + symbol.quote());
    }

    public SymbolNotFoundException(String message) {
        super(message);
    }
}
