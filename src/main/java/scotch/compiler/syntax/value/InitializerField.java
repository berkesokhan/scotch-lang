package scotch.compiler.syntax.value;

import static scotch.compiler.syntax.builder.BuilderUtil.require;

import java.util.Optional;
import lombok.EqualsAndHashCode;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.symbol.type.Type;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.text.SourceLocation;

@EqualsAndHashCode(callSuper = false)
public class InitializerField {

    public static Builder builder() {
        return new Builder();
    }

    public static InitializerField field(SourceLocation sourceLocation, String name, Value value) {
        return new InitializerField(sourceLocation, name, value);
    }

    private final SourceLocation sourceLocation;
    private final String         name;
    private final Value          value;

    InitializerField(SourceLocation sourceLocation, String name, Value value) {
        this.sourceLocation = sourceLocation;
        this.name = name;
        this.value = value;
    }

    public InitializerField accumulateDependencies(DependencyAccumulator state) {
        return withValue(value.accumulateDependencies(state));
    }

    public InitializerField accumulateNames(NameAccumulator state) {
        return withValue(value.accumulateNames(state));
    }

    public InitializerField bindMethods(TypeChecker state) {
        return withValue(value.bindMethods(state));
    }

    public InitializerField bindTypes(TypeChecker state) {
        return withValue(value.bindTypes(state));
    }

    public InitializerField checkTypes(TypeChecker state) {
        return withValue(value.checkTypes(state));
    }

    public InitializerField defineOperators(OperatorAccumulator state) {
        return withValue(value.defineOperators(state));
    }

    public CodeBlock generateBytecode(BytecodeGenerator state) {
        return new CodeBlock() {{
            value.generateBytecode(state);
        }};
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return value.getType();
    }

    public Value getValue() {
        return value;
    }

    public InitializerField parsePrecedence(PrecedenceParser state) {
        return withValue(value.parsePrecedence(state));
    }

    public InitializerField qualifyNames(ScopedNameQualifier state) {
        return withValue(value.qualifyNames(state));
    }

    @Override
    public String toString() {
        return "(" + name + " = " + value + ")";
    }

    public InitializerField withType(Type type) {
        return new InitializerField(sourceLocation, name, value.withType(type));
    }

    private InitializerField withValue(Value value) {
        return new InitializerField(sourceLocation, name, value);
    }

    public static class Builder implements SyntaxBuilder<InitializerField> {

        private Optional<SourceLocation> sourceLocation;
        private Optional<String>         name;
        private Optional<Value>          value;

        private Builder() {
            sourceLocation = Optional.empty();
            name = Optional.empty();
            value = Optional.empty();
        }

        @Override
        public InitializerField build() {
            return new InitializerField(
                require(sourceLocation, "Source location"),
                require(name, "Initializer field name"),
                require(value, "Initializer field value")
            );
        }

        public Builder withName(String name) {
            this.name = Optional.of(name);
            return this;
        }

        @Override
        public Builder withSourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = Optional.of(sourceLocation);
            return this;
        }

        public Builder withValue(Value value) {
            this.value = Optional.of(value);
            return this;
        }
    }
}
