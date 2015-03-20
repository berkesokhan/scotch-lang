package scotch.compiler.syntax.definition;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static scotch.compiler.output.GeneratedClass.ClassType.MODULE;
import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.compiler.syntax.reference.DefinitionReference.moduleRef;
import static scotch.util.StringUtil.stringify;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import lombok.EqualsAndHashCode;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.symbol.Symbol;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.text.SourceLocation;

@EqualsAndHashCode(callSuper = false)
public class ModuleDefinition extends Definition {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceLocation            sourceLocation;
    private final String                    symbol;
    private final List<Import>              imports;
    private final List<DefinitionReference> definitions;

    ModuleDefinition(SourceLocation sourceLocation, String symbol, List<Import> imports, List<DefinitionReference> definitions) {
        this.sourceLocation = sourceLocation;
        this.symbol = symbol;
        this.imports = ImmutableList.copyOf(imports);
        this.definitions = ImmutableList.copyOf(definitions);
    }

    @Override
    public Definition accumulateDependencies(DependencyAccumulator state) {
        return state.scoped(this, () -> withDefinitions(state.accumulateDependencies(definitions)));
    }

    @Override
    public Definition accumulateNames(NameAccumulator state) {
        return state.scoped(this, () -> withDefinitions(state.accumulateNames(definitions)));
    }

    @Override
    public Definition checkTypes(TypeChecker state) {
        return state.keep(this);
    }

    @Override
    public Definition defineOperators(OperatorAccumulator state) {
        return state.scoped(this, () -> withDefinitions(state.defineDefinitionOperators(definitions)));
    }

    @Override
    public void generateBytecode(BytecodeGenerator state) {
        state.scoped(this, () -> {
            state.beginClass(MODULE, Symbol.moduleClass(symbol), sourceLocation);
            state.defineDefaultConstructor(ACC_PRIVATE);
            state.generateBytecode(definitions);
            state.endClass();
            return null;
        });
    }

    public List<Import> getImports() {
        return imports;
    }

    @Override
    public DefinitionReference getReference() {
        return moduleRef(symbol);
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public Optional<Definition> parsePrecedence(PrecedenceParser state) {
        return Optional.of(state.scoped(this, () -> withDefinitions(new ArrayList<DefinitionReference>() {{
            addAll(state.mapOptional(definitions, Definition::parsePrecedence));
            addAll(state.processPatterns());
        }})));
    }

    @Override
    public Definition qualifyNames(ScopedNameQualifier state) {
        return state.scoped(this, () -> withDefinitions(state.qualifyDefinitionNames(definitions)));
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + symbol + ")";
    }

    public ModuleDefinition withDefinitions(List<DefinitionReference> definitions) {
        return new ModuleDefinition(sourceLocation, symbol, imports, definitions);
    }

    public static class Builder implements SyntaxBuilder<ModuleDefinition> {

        private Optional<String>                    symbol;
        private Optional<List<Import>>              imports;
        private Optional<List<DefinitionReference>> definitions;
        private Optional<SourceLocation>            sourceLocation;

        private Builder() {
            symbol = Optional.empty();
            imports = Optional.empty();
            definitions = Optional.empty();
            sourceLocation = Optional.empty();
        }

        @Override
        public ModuleDefinition build() {
            return Definitions.module(
                require(sourceLocation, "Source location"),
                require(symbol, "Module symbol"),
                require(imports, "Imports are required"),
                require(definitions, "Member definitions")
            );
        }

        public Builder withDefinitions(List<DefinitionReference> definitions) {
            this.definitions = Optional.of(definitions);
            return this;
        }

        public Builder withImports(List<Import> imports) {
            this.imports = Optional.of(imports);
            return this;
        }

        @Override
        public Builder withSourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = Optional.of(sourceLocation);
            return this;
        }

        public Builder withSymbol(String symbol) {
            this.symbol = Optional.of(symbol);
            return this;
        }
    }
}
