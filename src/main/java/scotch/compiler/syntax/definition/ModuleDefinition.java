package scotch.compiler.syntax.definition;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static scotch.compiler.symbol.Symbol.fromString;
import static scotch.compiler.syntax.reference.DefinitionReference.moduleRef;
import static scotch.util.StringUtil.stringify;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import scotch.compiler.symbol.NameQualifier;
import scotch.compiler.syntax.BytecodeGenerator;
import scotch.compiler.syntax.DependencyAccumulator;
import scotch.compiler.syntax.NameAccumulator;
import scotch.compiler.syntax.OperatorDefinitionParser;
import scotch.compiler.syntax.PrecedenceParser;
import scotch.compiler.syntax.TypeChecker;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.text.SourceRange;

public class ModuleDefinition extends Definition {

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
        return state.scoped(this, () -> withDefinitions(state.map(definitions, Definition::accumulateDependencies)));
    }

    @Override
    public Definition accumulateNames(NameAccumulator state) {
        return state.scoped(this, () -> withDefinitions(state.map(definitions, Definition::accumulateNames)));
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
    public Definition defineOperators(OperatorDefinitionParser state) {
        return state.scoped(this, () -> withDefinitions(state.map(definitions, Definition::defineOperators)));
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
            state.beginClass(getClassName(), sourceRange);
            state.defineDefaultConstructor(ACC_PRIVATE);
            state.map(definitions);
            state.endClass();
            return null;
        });
    }

    public String getClassName() {
        return fromString(symbol + ".ScotchModule").getClassName();
    }

    public List<DefinitionReference> getDefinitions() {
        return definitions;
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
        return state.scoped(this, () -> withDefinitions(state.map(definitions, Definition::qualifyNames)));
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + symbol + ")";
    }

    public ModuleDefinition withDefinitions(List<DefinitionReference> definitions) {
        return new ModuleDefinition(sourceRange, symbol, imports, definitions);
    }
}
