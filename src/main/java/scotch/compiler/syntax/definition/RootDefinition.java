package scotch.compiler.syntax.definition;

import static scotch.compiler.syntax.reference.DefinitionReference.rootRef;
import static scotch.util.StringUtil.stringify;

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

public class RootDefinition extends Definition {

    private final SourceRange               sourceRange;
    private final List<DefinitionReference> definitions;

    RootDefinition(SourceRange sourceRange, List<DefinitionReference> definitions) {
        this.sourceRange = sourceRange;
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
        return o == this || o instanceof RootDefinition && Objects.equals(definitions, ((RootDefinition) o).definitions);
    }

    @Override
    public void generateBytecode(BytecodeGenerator state) {
        state.generate(this, () -> state.map(definitions));
    }

    public List<DefinitionReference> getDefinitions() {
        return definitions;
    }

    @Override
    public DefinitionReference getReference() {
        return rootRef();
    }

    @Override
    public SourceRange getSourceRange() {
        return sourceRange;
    }

    @Override
    public int hashCode() {
        return Objects.hash(definitions);
    }

    @Override
    public Optional<Definition> parsePrecedence(PrecedenceParser state) {
        return Optional.of(state.scoped(this, () -> withDefinitions(state.mapOptional(definitions, Definition::parsePrecedence))));
    }

    @Override
    public Definition qualifyNames(NameQualifier state) {
        return state.scoped(this, () -> withDefinitions(state.map(definitions, Definition::qualifyNames)));
    }

    @Override
    public String toString() {
        return stringify(this);
    }

    public RootDefinition withDefinitions(List<DefinitionReference> definitions) {
        return new RootDefinition(sourceRange, definitions);
    }
}
