package scotch.compiler.syntax.definition;

import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import scotch.symbol.Symbol;
import scotch.symbol.SymbolResolver;
import scotch.symbol.descriptor.TypeInstanceDescriptor;
import scotch.symbol.type.Type;
import scotch.compiler.text.SourceLocation;

public abstract class Import {

    public static InclusionImport inclusionImport(SourceLocation sourceLocation, String moduleName, List<String> includes) {
        return new InclusionImport(sourceLocation, moduleName, includes);
    }

    public static ModuleImport moduleImport(SourceLocation sourceLocation, String moduleName) {
        return new ModuleImport(sourceLocation, moduleName);
    }

    protected static Set<Symbol> getContext_(String moduleName, Type type, SymbolResolver resolver) {
        return resolver.getTypeInstancesByModule(moduleName).stream()
            .filter(typeInstance -> typeInstance.getParameters().get(0).matches(type))
            .map(TypeInstanceDescriptor::getTypeClass)
            .collect(toSet());
    }

    Import() {
        // intentionally empty
    }

    @Override
    public abstract boolean equals(Object o);

    public abstract Set<Symbol> getContext(Type type, SymbolResolver resolver);

    @Override
    public abstract int hashCode();

    public abstract boolean isFrom(String moduleName);

    public abstract Optional<Symbol> qualify(String name, SymbolResolver resolver);

    @Override
    public abstract String toString();

    public abstract Import withSourceLocation(SourceLocation sourceLocation);
}
