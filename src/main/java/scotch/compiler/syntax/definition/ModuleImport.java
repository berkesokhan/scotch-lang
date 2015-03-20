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
import scotch.compiler.text.SourceLocation;

public final class ModuleImport extends Import {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceLocation sourceLocation;
    private final String         moduleName;

    ModuleImport(SourceLocation sourceLocation, String moduleName) {
        this.sourceLocation = sourceLocation;
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
    public ModuleImport withSourceLocation(SourceLocation sourceLocation) {
        return new ModuleImport(sourceLocation, moduleName);
    }

    public static class Builder implements SyntaxBuilder<ModuleImport> {

        private Optional<SourceLocation> sourceLocation;
        private Optional<String>         moduleName;

        private Builder() {
            moduleName = Optional.empty();
            sourceLocation = Optional.empty();
        }

        @Override
        public ModuleImport build() {
            return moduleImport(
                require(sourceLocation, "Source location"),
                require(moduleName, "Module name")
            );
        }

        public Builder withModuleName(String moduleName) {
            this.moduleName = Optional.of(moduleName);
            return this;
        }

        @Override
        public Builder withSourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = Optional.of(sourceLocation);
            return this;
        }
    }
}
