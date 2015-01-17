package scotch.compiler.syntax.builder;

import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.compiler.syntax.definition.Import.moduleImport;

import java.util.Optional;
import scotch.compiler.syntax.definition.Import;
import scotch.compiler.syntax.definition.ModuleImport;
import scotch.compiler.text.SourceRange;

public abstract class ImportBuilder<T extends Import> implements SyntaxBuilder<T> {

    public static ModuleImportBuilder moduleImportBuilder() {
        return new ModuleImportBuilder();
    }

    private ImportBuilder() {
        // intentionally empty
    }

    public abstract T build();

    public abstract ImportBuilder<T> withSourceRange(SourceRange sourceRange);

    public static class ModuleImportBuilder extends ImportBuilder<ModuleImport> {

        private Optional<SourceRange> sourceRange = Optional.empty();
        private Optional<String>      moduleName  = Optional.empty();

        @Override
        public ModuleImport build() {
            return moduleImport(
                require(sourceRange, "Source range"),
                require(moduleName, "Module name")
            );
        }

        public ModuleImportBuilder withModuleName(String moduleName) {
            this.moduleName = Optional.of(moduleName);
            return this;
        }

        @Override
        public ModuleImportBuilder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }
    }
}
