package scotch.compiler.syntax.definition;

import static scotch.compiler.symbol.Symbol.qualified;
import static scotch.util.StringUtil.stringify;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import com.google.common.collect.ImmutableList;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.SymbolResolver;
import scotch.compiler.symbol.Type;
import scotch.compiler.text.SourceRange;

public final class InclusionImport extends Import {

    private final SourceRange  sourceRange;
    private final String       moduleName;
    private final List<String> includes;

    InclusionImport(SourceRange sourceRange, String moduleName, List<String> includes) {
        this.sourceRange = sourceRange;
        this.moduleName = moduleName;
        this.includes = ImmutableList.copyOf(includes);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof InclusionImport) {
            InclusionImport other = (InclusionImport) o;
            return Objects.equals(moduleName, other.moduleName)
                && Objects.equals(includes, other.includes);
        } else {
            return false;
        }
    }

    @Override
    public Set<Symbol> getContext(Type type, SymbolResolver resolver) {
        return getContext_(moduleName, type, resolver);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moduleName, includes);
    }

    @Override
    public boolean isFrom(String moduleName) {
        return Objects.equals(this.moduleName, moduleName);
    }

    @Override
    public Optional<Symbol> qualify(String name, SymbolResolver resolver) {
        if (includes.contains(name) && resolver.isDefined(qualified(moduleName, name))) {
            return Optional.of(qualified(moduleName, name));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + moduleName + ", " + includes + ")";
    }

    public InclusionImport withSourceRange(SourceRange sourceRange) {
        return new InclusionImport(sourceRange, moduleName, includes);
    }
}
