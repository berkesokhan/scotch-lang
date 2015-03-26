package scotch.compiler.syntax.pattern;

import static lombok.AccessLevel.PACKAGE;
import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.compiler.syntax.value.Values.apply;
import static scotch.compiler.syntax.value.Values.id;
import static scotch.symbol.Symbol.symbol;
import static scotch.util.StringUtil.stringify;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.text.SourceLocation;
import scotch.runtime.Callable;
import scotch.runtime.RuntimeSupport;
import scotch.symbol.type.Type;

@AllArgsConstructor(access = PACKAGE)
@EqualsAndHashCode(callSuper = false)
public class EqualMatch extends PatternMatch {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceLocation   sourceLocation;
    private final Optional<String> argument;
    private final Value            value;

    @Override
    public PatternMatch accumulateDependencies(DependencyAccumulator state) {
        return withValue(value.accumulateDependencies(state));
    }

    @Override
    public PatternMatch accumulateNames(NameAccumulator state) {
        return this;
    }

    @Override
    public PatternMatch bind(String argument, Scope scope) {
        if (this.argument.isPresent()) {
            throw new IllegalStateException();
        } else {
            return new EqualMatch(sourceLocation, Optional.of(argument), apply(
                apply(
                    id(sourceLocation, symbol("scotch.data.eq.(==)"), scope.reserveType()),
                    id(sourceLocation, symbol(argument), scope.reserveType()),
                    scope.reserveType()
                ),
                value,
                scope.reserveType()
            ));
        }
    }

    @Override
    public PatternMatch bindMethods(TypeChecker state) {
        return withValue(value.bindMethods(state));
    }

    @Override
    public PatternMatch bindTypes(TypeChecker state) {
        return withValue(value.bindTypes(state));
    }

    @Override
    public PatternMatch checkTypes(TypeChecker state) {
        return withValue(value.checkTypes(state));
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        return new CodeBlock() {{
            append(value.generateBytecode(state));
            invokestatic(p(RuntimeSupport.class), "unboxBool", sig(boolean.class, Callable.class));
            iffalse(state.nextCase());
        }};
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public Type getType() {
        return value.getType();
    }

    public Value getValue() {
        return value;
    }

    @Override
    public PatternMatch qualifyNames(ScopedNameQualifier state) {
        return withValue(value.qualifyNames(state));
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + value + ")";
    }

    public EqualMatch withSourceLocation(SourceLocation sourceLocation) {
        return new EqualMatch(sourceLocation, argument, value);
    }

    @Override
    public EqualMatch withType(Type type) {
        return new EqualMatch(sourceLocation, argument, value);
    }

    public EqualMatch withValue(Value value) {
        return new EqualMatch(sourceLocation, argument, value);
    }

    public static class Builder implements SyntaxBuilder<EqualMatch> {

        private Optional<Value>          value;
        private Optional<SourceLocation> sourceLocation;

        private Builder() {
            // intentionally empty
        }

        @Override
        public EqualMatch build() {
            return Patterns.equal(
                require(sourceLocation, "Source location"),
                Optional.empty(),
                require(value, "Capture value")
            );
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
