package scotch.compiler.syntax.definition;

import static scotch.symbol.Operator.operator;
import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.compiler.syntax.reference.DefinitionReference.operatorRef;
import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import scotch.compiler.intermediate.IntermediateGenerator;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.symbol.Operator;
import scotch.symbol.Symbol;
import scotch.symbol.Value.Fixity;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.text.SourceLocation;

public class OperatorDefinition extends Definition {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceLocation sourceLocation;
    private final Symbol         symbol;
    private final Fixity         fixity;
    private final int            precedence;

    OperatorDefinition(SourceLocation sourceLocation, Symbol symbol, Fixity fixity, int precedence) {
        this.sourceLocation = sourceLocation;
        this.symbol = symbol;
        this.fixity = fixity;
        this.precedence = precedence;
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
        return state.scoped(this, () -> {
            state.defineOperator(symbol, getOperator());
            return this;
        });
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof OperatorDefinition) {
            OperatorDefinition other = (OperatorDefinition) o;
            return Objects.equals(symbol, other.symbol)
                && Objects.equals(fixity, other.fixity)
                && Objects.equals(precedence, other.precedence);
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

    public Operator getOperator() {
        return operator(fixity, precedence);
    }

    @Override
    public DefinitionReference getReference() {
        return operatorRef(symbol);
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, fixity, precedence);
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
        return stringify(this) + "(" + symbol + " :: " + fixity + ", " + precedence + ")";
    }

    public static class Builder implements SyntaxBuilder<OperatorDefinition> {

        private Optional<Symbol>         symbol;
        private Optional<Fixity>         fixity;
        private OptionalInt              precedence;
        private Optional<SourceLocation> sourceLocation;

        private Builder() {
            symbol = Optional.empty();
            fixity = Optional.empty();
            precedence = OptionalInt.empty();
            sourceLocation = Optional.empty();
        }

        @Override
        public OperatorDefinition build() {
            return Definitions.operatorDef(
                require(sourceLocation, "Source location"),
                require(symbol, "Operator symbol"),
                require(fixity, "Operator fixity"),
                require(precedence, "Operator precedence")
            );
        }

        public Builder withFixity(Fixity fixity) {
            this.fixity = Optional.of(fixity);
            return this;
        }

        public Builder withPrecedence(int precedence) {
            this.precedence = OptionalInt.of(precedence);
            return this;
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
