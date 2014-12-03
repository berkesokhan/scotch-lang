package scotch.compiler.symbol;

import java.util.Optional;

public interface SymbolResolver {

    Optional<SymbolEntry> getEntry(Symbol symbol);

    default boolean isDefined(Symbol symbol) {
        return getEntry(symbol).isPresent();
    }
}
