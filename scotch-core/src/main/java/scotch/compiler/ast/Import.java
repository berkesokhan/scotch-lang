package scotch.compiler.ast;

import static scotch.compiler.util.TextUtil.stringify;

import java.util.Objects;
import java.util.Optional;
import scotch.compiler.analyzer.SymbolResolver;

public abstract class Import {

    public static Import moduleImport(String moduleName) {
        return new ModuleImport(moduleName);
    }

    private Import() {
        // intentionally empty
    }

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    public abstract Optional<String> qualify(String name, SymbolResolver resolver);

    @Override
    public abstract String toString();

    public static final class ModuleImport extends Import {

        private final String moduleName;

        public ModuleImport(String moduleName) {
            this.moduleName = moduleName;
        }

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof ModuleImport && Objects.equals(moduleName, ((ModuleImport) o).moduleName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(moduleName);
        }

        @Override
        public Optional<String> qualify(String name, SymbolResolver resolver) {
            return resolver.qualify(moduleName, name);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + moduleName + ")";
        }
    }
}
