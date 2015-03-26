package scotch.compiler.syntax.definition;

import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.compiler.syntax.reference.DefinitionReference.scopeRef;
import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import java.util.Optional;
import scotch.compiler.intermediate.IntermediateGenerator;
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

public class ScopeDefinition extends Definition {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceLocation sourceLocation;
    private final Symbol         symbol;

    ScopeDefinition(SourceLocation sourceLocation, Symbol symbol) {
        this.sourceLocation = sourceLocation;
        this.symbol = symbol;
    }

    @Override
    public Definition accumulateDependencies(DependencyAccumulator state) {
        return state.keep(this);
    }

    @Override
    public Definition accumulateNames(NameAccumulator state) {
        return state.keep(this);
    }

    @Override
    public Definition checkTypes(TypeChecker state) {
        return state.keep(this);
    }

    @Override
    public Definition defineOperators(OperatorAccumulator state) {
        return state.keep(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof ScopeDefinition) {
            ScopeDefinition other = (ScopeDefinition) o;
            return Objects.equals(sourceLocation, other.sourceLocation)
                && Objects.equals(symbol, other.symbol);
        } else {
            return false;
        }
    }

    @Override
    public void generateBytecode(BytecodeGenerator state) {
        // intentionally empty
    }

    @Override
    public void generateIntermediateCode(IntermediateGenerator state) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public DefinitionReference getReference() {
        return scopeRef(symbol);
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol);
    }

    @Override
    public Optional<Definition> parsePrecedence(PrecedenceParser state) {
        return Optional.of(state.keep(this));
    }

    @Override
    public Definition qualifyNames(ScopedNameQualifier state) {
        return state.keep(this);
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + symbol.getCanonicalName() + ")";
    }

    public static class Builder implements SyntaxBuilder<ScopeDefinition> {

        private Optional<Symbol>         symbol;
        private Optional<SourceLocation> sourceLocation;

        private Builder() {
            symbol = Optional.empty();
            sourceLocation = Optional.empty();
        }

        @Override
        public ScopeDefinition build() {
            return Definitions.scopeDef(
                require(sourceLocation, "Source location"),
                require(symbol, "Scope symbol")
            );
        }

        @Override
        public Builder withSourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = Optional.of(sourceLocation);
            return this;
        }

        public Builder withSymbol(Symbol symbol) {
            this.symbol = Optional.of(symbol);
            return this;
        }
    }
}
