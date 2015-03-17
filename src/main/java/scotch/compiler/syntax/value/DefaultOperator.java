package scotch.compiler.syntax.value;

import static scotch.compiler.symbol.Operator.operator;
import static scotch.compiler.symbol.Value.Fixity.LEFT_INFIX;
import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.compiler.util.Pair.pair;

import java.util.Objects;
import java.util.Optional;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.symbol.Operator;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.text.SourceRange;
import scotch.compiler.util.Pair;

public class DefaultOperator extends Value {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceRange sourceRange;
    private final Symbol      symbol;
    private final Type        type;

    DefaultOperator(SourceRange sourceRange, Symbol symbol, Type type) {
        this.sourceRange = sourceRange;
        this.symbol = symbol;
        this.type = type;
    }

    @Override
    public Value accumulateDependencies(DependencyAccumulator state) {
        throw new UnsupportedOperationException();
    }

    private Identifier asIdentifier() {
        return Identifier.builder()
            .withSourceRange(sourceRange)
            .withSymbol(symbol)
            .withType(type)
            .build();
    }

    @Override
    public Value accumulateNames(NameAccumulator state) {
        throw new UnsupportedOperationException();
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
            return Objects.equals(sourceRange, other.sourceRange)
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
    public SourceRange getSourceRange() {
        return sourceRange;
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

        private Optional<SourceRange> sourceRange = Optional.empty();
        private Optional<Symbol>      symbol      = Optional.empty();
        private Optional<Type>        type        = Optional.empty();

        @Override
        public DefaultOperator build() {
            return new DefaultOperator(
                require(sourceRange, "Source range"),
                require(symbol, "Default operator symbol"),
                require(type, "Default operator type")
            );
        }

        @Override
        public Builder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
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
