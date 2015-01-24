package scotch.compiler.syntax.value;

import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import java.util.Optional;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.syntax.BytecodeGenerator;
import scotch.compiler.syntax.DependencyAccumulator;
import scotch.compiler.syntax.NameAccumulator;
import scotch.compiler.syntax.NameQualifier;
import scotch.compiler.syntax.OperatorDefinitionParser;
import scotch.compiler.syntax.PrecedenceParser;
import scotch.compiler.syntax.TypeChecker;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.text.SourceRange;

public class InitializerField {

    public static Builder builder() {
        return new Builder();
    }

    public static InitializerField field(SourceRange sourceRange, String name, Value value) {
        return new InitializerField(sourceRange, name, value);
    }

    private final SourceRange sourceRange;
    private final String      name;
    private final Value       value;

    InitializerField(SourceRange sourceRange, String name, Value value) {
        this.sourceRange = sourceRange;
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

    public InitializerField defineOperators(OperatorDefinitionParser state) {
        return withValue(value.defineOperators(state));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof InitializerField) {
            InitializerField other = (InitializerField) o;
            return Objects.equals(sourceRange, other.sourceRange)
                && Objects.equals(name, other.name)
                && Objects.equals(value, other.value);
        } else {
            return false;
        }
    }

    public CodeBlock generateBytecode(BytecodeGenerator state) {
        return new CodeBlock() {{
            value.generateBytecode(state);
        }};
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }

    public InitializerField parsePrecedence(PrecedenceParser state) {
        return withValue(value.parsePrecedence(state));
    }

    public InitializerField qualifyNames(NameQualifier state) {
        return withValue(value.qualifyNames(state));
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + name + ")";
    }

    private InitializerField withValue(Value value) {
        return new InitializerField(sourceRange, name, value);
    }

    public static class Builder implements SyntaxBuilder<InitializerField> {

        private Optional<SourceRange> sourceRange;
        private Optional<String>      name;
        private Optional<Value>       value;

        private Builder() {
            sourceRange = Optional.empty();
            name = Optional.empty();
            value = Optional.empty();
        }

        @Override
        public InitializerField build() {
            return new InitializerField(
                require(sourceRange, "Source range"),
                require(name, "Initializer field name"),
                require(value, "Initializer field value")
            );
        }

        public Builder withName(String name) {
            this.name = Optional.of(name);
            return this;
        }

        @Override
        public Builder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }

        public Builder withValue(Value value) {
            this.value = Optional.of(value);
            return this;
        }
    }
}
