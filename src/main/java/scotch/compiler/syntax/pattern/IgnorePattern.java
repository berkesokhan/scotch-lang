package scotch.compiler.syntax.pattern;

import static lombok.AccessLevel.PACKAGE;
import static scotch.compiler.syntax.builder.BuilderUtil.require;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.symbol.type.Type;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.text.SourceLocation;

@AllArgsConstructor(access = PACKAGE)
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "sourceLocation")
public class IgnorePattern extends PatternMatch {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceLocation sourceLocation;
    private final Type           type;

    @Override
    public PatternMatch accumulateDependencies(DependencyAccumulator state) {
        return this;
    }

    @Override
    public PatternMatch accumulateNames(NameAccumulator state) {
        return this;
    }

    @Override
    public PatternMatch bind(String argument, Scope scope) {
        return this;
    }

    @Override
    public PatternMatch bindMethods(TypeChecker state) {
        return this;
    }

    @Override
    public PatternMatch bindTypes(TypeChecker state) {
        return withType(state.generate(type));
    }

    @Override
    public PatternMatch checkTypes(TypeChecker state) {
        return this;
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        return new CodeBlock();
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
    public PatternMatch qualifyNames(ScopedNameQualifier state) {
        return this;
    }

    @Override
    public PatternMatch withType(Type type) {
        return new IgnorePattern(sourceLocation, type);
    }

    public static class Builder implements SyntaxBuilder<IgnorePattern> {

        private Optional<SourceLocation> sourceLocation = Optional.empty();
        private Optional<Type>           type        = Optional.empty();

        private Builder() {
            // intentionally empty
        }

        @Override
        public IgnorePattern build() {
            return new IgnorePattern(
                require(sourceLocation, "Source location"),
                require(type, "Type")
            );
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
