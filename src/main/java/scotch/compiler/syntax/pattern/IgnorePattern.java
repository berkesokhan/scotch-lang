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
import scotch.compiler.symbol.type.Type;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.text.SourceRange;

@AllArgsConstructor(access = PACKAGE)
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "sourceRange")
public class IgnorePattern extends PatternMatch {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceRange sourceRange;
    private final Type        type;

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
    public SourceRange getSourceRange() {
        return sourceRange;
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
        return new IgnorePattern(sourceRange, type);
    }

    public static class Builder implements SyntaxBuilder<IgnorePattern> {

        private Optional<SourceRange> sourceRange = Optional.empty();
        private Optional<Type>        type        = Optional.empty();

        private Builder() {
            // intentionally empty
        }

        @Override
        public IgnorePattern build() {
            return new IgnorePattern(
                require(sourceRange, "Source range"),
                require(type, "Type")
            );
        }

        @Override
        public Builder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }

        public Builder withType(Type type) {
            this.type = Optional.of(type);
            return this;
        }
    }
}
