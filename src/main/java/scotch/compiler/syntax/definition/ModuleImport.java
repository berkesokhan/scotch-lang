package scotch.compiler.syntax.definition;

import static scotch.compiler.symbol.Symbol.qualified;
import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.SymbolResolver;
import scotch.compiler.symbol.Type;
import scotch.compiler.text.SourceRange;

public final class ModuleImport extends Import {

    private final SourceRange sourceRange;
    private final String      moduleName;

    ModuleImport(SourceRange sourceRange, String moduleName) {
        this.sourceRange = sourceRange;
        this.moduleName = moduleName;
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof ModuleImport && Objects.equals(moduleName, ((ModuleImport) o).moduleName);
    }

    @Override
    public Set<Symbol> getContext(Type type, SymbolResolver resolver) {
        return getContext_(moduleName, type, resolver);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moduleName);
    }

    @Override
    public boolean isFrom(String moduleName) {
        return Objects.equals(this.moduleName, moduleName);
    }

    @Override
    public Optional<Symbol> qualify(String name, SymbolResolver resolver) {
        if (resolver.isDefined(qualified(moduleName, name))) {
            return Optional.of(qualified(moduleName, name));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + moduleName + ")";
    }

    public ModuleImport withSourceRange(SourceRange sourceRange) {
        return new ModuleImport(sourceRange, moduleName);
    }
}
