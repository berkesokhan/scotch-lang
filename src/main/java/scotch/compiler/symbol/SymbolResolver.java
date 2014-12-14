package scotch.compiler.symbol;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SymbolResolver {

    Optional<SymbolEntry> getEntry(Symbol symbol);

    Set<TypeInstanceDescriptor> getTypeInstances(Symbol symbol, List<Type> types);

    Set<TypeInstanceDescriptor> getTypeInstancesByModule(String moduleName);

    default boolean isDefined(Symbol symbol) {
        return getEntry(symbol).isPresent();
    }
}
