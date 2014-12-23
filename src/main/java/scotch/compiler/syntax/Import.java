package scotch.compiler.syntax;

import static java.util.stream.Collectors.toSet;
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
import scotch.compiler.symbol.TypeInstanceDescriptor;
import scotch.compiler.text.SourceRange;

public abstract class Import {

    public static InclusionImport inclusionImport(SourceRange sourceRange, String moduleName, List<String> includes) {
        return new InclusionImport(sourceRange, moduleName, includes);
    }

    public static ModuleImport moduleImport(SourceRange sourceRange, String moduleName) {
        return new ModuleImport(sourceRange, moduleName);
    }

    private static Set<Symbol> getContext_(String moduleName, Type type, SymbolResolver resolver) {
        return resolver.getTypeInstancesByModule(moduleName).stream()
            .filter(typeInstance -> typeInstance.getParameters().get(0).equals(type))
            .map(TypeInstanceDescriptor::getTypeClass)
            .collect(toSet());
    }

    private Import() {
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

    public static final class InclusionImport extends Import {

        private final SourceRange  sourceRange;
        private final String       moduleName;
        private final List<String> includes;

        public InclusionImport(SourceRange sourceRange, String moduleName, List<String> includes) {
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

    public static final class ModuleImport extends Import {

        private final SourceRange sourceRange;
        private final String      moduleName;

        public ModuleImport(SourceRange sourceRange, String moduleName) {
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
}
