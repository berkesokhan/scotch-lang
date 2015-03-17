package scotch.compiler.syntax.definition;

import static scotch.symbol.Symbol.qualified;
import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import scotch.symbol.Symbol;
import scotch.symbol.SymbolResolver;
import scotch.symbol.type.Type;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.text.SourceRange;

public final class ModuleImport extends Import {

    public static Builder builder() {
        return new Builder();
    }

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

    @Override
    public ModuleImport withSourceRange(SourceRange sourceRange) {
        return new ModuleImport(sourceRange, moduleName);
    }

    public static class Builder implements SyntaxBuilder<ModuleImport> {

        private Optional<SourceRange> sourceRange;
        private Optional<String>      moduleName;

        private Builder() {
            moduleName = Optional.empty();
            sourceRange = Optional.empty();
        }

        @Override
        public ModuleImport build() {
            return moduleImport(
                require(sourceRange, "Source range"),
                require(moduleName, "Module name")
            );
        }

        public Builder withModuleName(String moduleName) {
            this.moduleName = Optional.of(moduleName);
            return this;
        }

        @Override
        public Builder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }
    }
}
