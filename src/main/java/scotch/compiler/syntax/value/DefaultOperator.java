package scotch.compiler.syntax.value;

import static scotch.symbol.Operator.operator;
import static scotch.symbol.Value.Fixity.LEFT_INFIX;
import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.compiler.util.Pair.pair;

import java.util.Objects;
import java.util.Optional;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.intermediate.IntermediateGenerator;
import scotch.compiler.intermediate.IntermediateValue;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.symbol.Operator;
import scotch.symbol.Symbol;
import scotch.symbol.type.Type;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.text.SourceLocation;
import scotch.compiler.util.Pair;

public class DefaultOperator extends Value {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceLocation sourceLocation;
    private final Symbol         symbol;
    private final Type           type;

    DefaultOperator(SourceLocation sourceLocation, Symbol symbol, Type type) {
        this.sourceLocation = sourceLocation;
        this.symbol = symbol;
        this.type = type;
    }

    @Override
    public Value accumulateDependencies(DependencyAccumulator state) {
        throw new UnsupportedOperationException();
    }

    private Identifier asIdentifier() {
        return Identifier.builder()
            .withSourceLocation(sourceLocation)
            .withSymbol(symbol)
            .withType(type)
            .build();
    }

    @Override
    public Value accumulateNames(NameAccumulator state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IntermediateValue generateIntermediateCode(IntermediateGenerator state) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public Optional<Pair<Identifier, Operator>> asOperator(Scope scope) {
        if (scope.isOperator(symbol)) {
            return scope.qualify(symbol)
                .flatMap(scope::getOperator)
                .map(operator -> pair(asIdentifier(), operator));
        } else {
            return Optional.of(pair(asIdentifier(), operator(LEFT_INFIX, 20)));
        }
    }

    @Override
    public Value bindMethods(TypeChecker state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value bindTypes(TypeChecker state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value checkTypes(TypeChecker state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value defineOperators(OperatorAccumulator state) {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof DefaultOperator) {
            DefaultOperator other = (DefaultOperator) o;
            return Objects.equals(sourceLocation, other.sourceLocation)
                && Objects.equals(symbol, other.symbol)
                && Objects.equals(type, other.type);
        } else {
            return false;
        }
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, type);
    }

    @Override
    public boolean isOperator(Scope scope) {
        return true;
    }

    @Override
    public Value parsePrecedence(PrecedenceParser state) {
        return this;
    }

    @Override
    public Value qualifyNames(ScopedNameQualifier state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "`" + symbol + "`";
    }

    @Override
    public Value withType(Type type) {
        throw new UnsupportedOperationException();
    }

    public static class Builder implements SyntaxBuilder<DefaultOperator> {

        private Optional<SourceLocation> sourceLocation = Optional.empty();
        private Optional<Symbol>         symbol      = Optional.empty();
        private Optional<Type>           type        = Optional.empty();

        @Override
        public DefaultOperator build() {
            return new DefaultOperator(
                require(sourceLocation, "Source location"),
                require(symbol, "Default operator symbol"),
                require(type, "Default operator type")
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

        public Builder withType(Type type) {
            this.type = Optional.of(type);
            return this;
        }
    }
}
