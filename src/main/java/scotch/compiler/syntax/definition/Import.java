package scotch.compiler.syntax.definition;

import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.SymbolResolver;
import scotch.compiler.symbol.descriptor.TypeInstanceDescriptor;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.text.SourceRange;

public abstract class Import {

    public static InclusionImport inclusionImport(SourceRange sourceRange, String moduleName, List<String> includes) {
        return new InclusionImport(sourceRange, moduleName, includes);
    }

    public static ModuleImport moduleImport(SourceRange sourceRange, String moduleName) {
        return new ModuleImport(sourceRange, moduleName);
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
}
