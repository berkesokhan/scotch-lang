package scotch.compiler.syntax.value;

import static scotch.symbol.Symbol.unqualified;
import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.compiler.syntax.value.Values.arg;

import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.symbol.Symbol;
import scotch.symbol.type.Type;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.text.SourceLocation;

@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "sourceLocation")
public class Argument extends Value {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceLocation sourceLocation;
    private final String         name;
    private final Type           type;

    Argument(SourceLocation sourceLocation, String name, Type type) {
        this.sourceLocation = sourceLocation;
        this.name = name;
        this.type = type;
    }

    @Override
    public Argument accumulateDependencies(DependencyAccumulator state) {
        return this;
    }

    @Override
    public Argument accumulateNames(NameAccumulator state) {
        state.defineValue(getSymbol(), type);
        return this;
    }

    @Override
    public Argument bindMethods(TypeChecker state) {
        return this;
    }

    @Override
    public Argument bindTypes(TypeChecker state) {
        return withType(state.generate(getType()));
    }

    @Override
    public Argument checkTypes(TypeChecker state) {
        state.capture(getSymbol());
        return this;
    }

    @Override
    public Argument defineOperators(OperatorAccumulator state) {
        return this;
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        return new CodeBlock() {{
            aload(state.getVariable(name));
        }};
    }

    public String getName() {
        return name;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    public Symbol getSymbol() {
        return unqualified(name);
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Argument parsePrecedence(PrecedenceParser state) {
        return this;
    }

    @Override
    public Argument qualifyNames(ScopedNameQualifier state) {
        return new Argument(sourceLocation, name, type.qualifyNames(state));
    }

    @Override
    public Argument withType(Type type) {
        return arg(sourceLocation, name, type);
    }

    public static class Builder implements SyntaxBuilder<Argument> {

        private Optional<String>         name;
        private Optional<Type>           type;
        private Optional<SourceLocation> sourceLocation;

        private Builder() {
            name = Optional.empty();
            type = Optional.empty();
            sourceLocation = Optional.empty();
        }

        @Override
        public Argument build() {
            return arg(
                require(sourceLocation, "Source location"),
                require(name, "Argument name"),
                require(type, "Argument type")
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

        public Builder withType(Type type) {
            this.type = Optional.of(type);
            return this;
        }
    }
}
