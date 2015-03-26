package scotch.symbol;

import static java.util.Arrays.asList;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import scotch.symbol.descriptor.TypeInstanceDescriptor;
import scotch.symbol.type.SumType;
import scotch.symbol.type.Type;

public interface SymbolResolver {

    Optional<SymbolEntry> getEntry(Symbol symbol);

    Set<TypeInstanceDescriptor> getTypeInstances(Symbol symbol, List<Type> types);

    Set<TypeInstanceDescriptor> getTypeInstancesByModule(String moduleName);

    default boolean isDefined(Symbol symbol) {
        return getEntry(symbol).isPresent();
    }

    default boolean isImplemented(Symbol typeClass, SumType type) {
        return !getTypeInstances(typeClass, asList(type)).isEmpty();
    }
}
