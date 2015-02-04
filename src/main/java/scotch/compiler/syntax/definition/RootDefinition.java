package scotch.compiler.syntax.definition;

import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.compiler.syntax.reference.DefinitionReference.rootRef;
import static scotch.util.StringUtil.stringify;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.NameQualifier;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.text.SourceRange;

public class RootDefinition extends Definition {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceRange               sourceRange;
    private final List<DefinitionReference> definitions;

    RootDefinition(SourceRange sourceRange, List<DefinitionReference> definitions) {
        this.sourceRange = sourceRange;
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
        return o == this || o instanceof RootDefinition && Objects.equals(definitions, ((RootDefinition) o).definitions);
    }

    @Override
    public void generateBytecode(BytecodeGenerator state) {
        state.generate(this, () -> state.generateBytecode(definitions));
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
        return state.scoped(this, () -> withDefinitions(state.qualifyDefinitionNames(definitions)));
    }

    @Override
    public String toString() {
        return stringify(this);
    }

    public RootDefinition withDefinitions(List<DefinitionReference> definitions) {
        return new RootDefinition(sourceRange, definitions);
    }

    public static class Builder implements SyntaxBuilder<RootDefinition> {

        private List<DefinitionReference> definitions;
        private Optional<SourceRange>     sourceRange;

        private Builder() {
            definitions = new ArrayList<>();
            sourceRange = Optional.empty();
        }

        @Override
        public RootDefinition build() {
            return Definitions.root(require(sourceRange, "Source range"), definitions);
        }

        public Builder withModule(DefinitionReference module) {
            definitions.add(module);
            return this;
        }

        @Override
        public Builder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }
    }
}
