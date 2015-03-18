package scotch.compiler.syntax.value;

import static lombok.AccessLevel.PACKAGE;
import static scotch.compiler.syntax.builder.BuilderUtil.require;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.text.SourceRange;
import scotch.symbol.FieldSignature;
import scotch.symbol.Symbol;
import scotch.symbol.type.Type;

@AllArgsConstructor(access = PACKAGE)
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "sourceRange")
public class ConstantReference extends Value {

    public static Builder builder() {
        return new Builder();
    }

    @Getter
    private final SourceRange    sourceRange;
    private final Symbol         symbol;
    private final Symbol         dataType;
    private final FieldSignature constantField;
    @Getter
    private final Type           type;

    @Override
    public Value accumulateDependencies(DependencyAccumulator state) {
        return this;
    }

    @Override
    public Value accumulateNames(NameAccumulator state) {
        return this;
    }

    @Override
    public Value bindMethods(TypeChecker state) {
        return this;
    }

    @Override
    public Value bindTypes(TypeChecker state) {
        return withType(state.generate(type));
    }

    @Override
    public Value checkTypes(TypeChecker state) {
        return this;
    }

    @Override
    public Value defineOperators(OperatorAccumulator state) {
        return this;
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        return constantField.getValue();
    }

    @Override
    public Value parsePrecedence(PrecedenceParser state) {
        return this;
    }

    @Override
    public Value qualifyNames(ScopedNameQualifier state) {
        return this;
    }

    @Override
    public Value withType(Type type) {
        return new ConstantReference(sourceRange, symbol, dataType, constantField, type);
    }

    public static class Builder implements SyntaxBuilder<ConstantReference> {

        private Optional<SourceRange>    sourceRange   = Optional.empty();
        private Optional<Symbol>         symbol        = Optional.empty();
        private Optional<Symbol>         dataType      = Optional.empty();
        private Optional<Type>           type          = Optional.empty();
        private Optional<FieldSignature> constantField = Optional.empty();

        private Builder() {
            // intentionally empty
        }

        @Override
        public ConstantReference build() {
            return new ConstantReference(
                require(sourceRange, "Source range"),
                require(symbol, "Symbol"),
                require(dataType, "Data type"),
                require(constantField, "Constant field"),
                require(type, "Type")
            );
        }

        public Builder withConstantField(FieldSignature constantField) {
            this.constantField = Optional.of(constantField);
            return this;
        }

        public Builder withDataType(Symbol dataType) {
            this.dataType = Optional.of(dataType);
            return this;
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
