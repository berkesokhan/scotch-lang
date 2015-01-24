package scotch.compiler.syntax.definition;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.compiler.syntax.reference.DefinitionReference.moduleRef;
import static scotch.util.StringUtil.stringify;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulatorState;
import scotch.compiler.steps.NameQualifier;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.text.SourceRange;

public class ModuleDefinition extends Definition {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceRange               sourceRange;
    private final String                    symbol;
    private final List<Import>              imports;
    private final List<DefinitionReference> definitions;

    ModuleDefinition(SourceRange sourceRange, String symbol, List<Import> imports, List<DefinitionReference> definitions) {
        this.sourceRange = sourceRange;
        this.symbol = symbol;
        this.imports = ImmutableList.copyOf(imports);
        this.definitions = ImmutableList.copyOf(definitions);
    }

    @Override
    public Definition accumulateDependencies(DependencyAccumulator state) {
        return state.scoped(this, () -> withDefinitions(state.accumulateDependencies(definitions)));
    }

    @Override
    public Definition accumulateNames(NameAccumulatorState state) {
        return state.scoped(this, () -> withDefinitions(state.accumulateNames(definitions)));
    }

    @Override
    public Definition bindTypes(TypeChecker state) {
        throw new UnsupportedOperationException();
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
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof ModuleDefinition) {
            ModuleDefinition other = (ModuleDefinition) o;
            return Objects.equals(symbol, other.symbol)
                && Objects.equals(imports, other.imports)
                && Objects.equals(definitions, other.definitions);
        } else {
            return false;
        }
    }

    @Override
    public void generateBytecode(BytecodeGenerator state) {
        state.scoped(this, () -> {
            state.beginClass(Symbol.moduleClass(symbol), sourceRange);
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
    public SourceRange getSourceRange() {
        return sourceRange;
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, imports, definitions);
    }

    @Override
    public Optional<Definition> parsePrecedence(PrecedenceParser state) {
        return Optional.of(state.scoped(this, () -> withDefinitions(new ArrayList<DefinitionReference>() {{
            addAll(state.mapOptional(definitions, Definition::parsePrecedence));
            addAll(state.processPatterns());
        }})));
    }

    @Override
    public Definition qualifyNames(NameQualifier state) {
        return state.scoped(this, () -> withDefinitions(state.qualifyDefinitionNames(definitions)));
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + symbol + ")";
    }

    public ModuleDefinition withDefinitions(List<DefinitionReference> definitions) {
        return new ModuleDefinition(sourceRange, symbol, imports, definitions);
    }

    public static class Builder implements SyntaxBuilder<ModuleDefinition> {

        private Optional<String>                    symbol;
        private Optional<List<Import>>              imports;
        private Optional<List<DefinitionReference>> definitions;
        private Optional<SourceRange>               sourceRange;

        private Builder() {
            symbol = Optional.empty();
            imports = Optional.empty();
            definitions = Optional.empty();
            sourceRange = Optional.empty();
        }

        @Override
        public ModuleDefinition build() {
            return module(
                require(sourceRange, "Source range"),
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
        public Builder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }

        public Builder withSymbol(String symbol) {
            this.symbol = Optional.of(symbol);
            return this;
        }
    }
}
