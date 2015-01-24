package scotch.compiler.syntax.value;

import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static scotch.compiler.symbol.type.Type.sum;
import static scotch.compiler.syntax.TypeError.typeError;
import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import me.qmx.jitescript.CodeBlock;
import org.objectweb.asm.tree.LabelNode;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulatorState;
import scotch.compiler.steps.NameQualifier;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.text.SourceRange;
import scotch.runtime.Callable;

public class Conditional extends Value {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceRange sourceRange;
    private final Value       condition;
    private final Value       whenTrue;
    private final Value       whenFalse;
    private final Type        type;

    Conditional(SourceRange sourceRange, Value condition, Value whenTrue, Value whenFalse, Type type) {
        this.sourceRange = sourceRange;
        this.condition = condition;
        this.whenTrue = whenTrue;
        this.whenFalse = whenFalse;
        this.type = type;
    }

    @Override
    public Value accumulateDependencies(DependencyAccumulator state) {
        return parse(state, Value::accumulateDependencies);
    }

    @Override
    public Value accumulateNames(NameAccumulatorState state) {
        return parse(state, Value::accumulateNames);
    }

    @Override
    public Value bindMethods(TypeChecker state) {
        return parse(state, Value::bindMethods);
    }

    @Override
    public Value bindTypes(TypeChecker state) {
        return parse(state, Value::bindTypes);
    }

    @Override
    public Value checkTypes(TypeChecker state) {
        Value c = condition.checkTypes(state);
        Value t = whenTrue.checkTypes(state);
        Value f = whenFalse.checkTypes(state);
        Type resultType = sum("scotch.data.bool.Bool").unify(c.getType(), state)
            .map(ct -> t.getType().unify(f.getType(), state))
            .orElseGet(unification -> {
                state.error(typeError(unification, sourceRange));
                return type;
            });
        return conditional(sourceRange, c, t, f, resultType);
    }

    @Override
    public Value defineOperators(OperatorAccumulator state) {
        return parse(state, Value::defineOperators);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof Conditional) {
            Conditional other = (Conditional) o;
            return Objects.equals(sourceRange, other.sourceRange)
                && Objects.equals(condition, other.condition)
                && Objects.equals(whenTrue, other.whenTrue)
                && Objects.equals(whenFalse, other.whenFalse)
                && Objects.equals(type, other.type);
        } else {
            return false;
        }
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        return new CodeBlock() {{
            LabelNode falseBranch = new LabelNode();
            LabelNode end = new LabelNode();
            append(condition.generateBytecode(state));
            invokestatic(p(Callable.class), "unboxBool", sig(boolean.class, Callable.class));
            iffalse(falseBranch);
            append(whenTrue.generateBytecode(state));
            go_to(end);
            label(falseBranch);
            append(whenFalse.generateBytecode(state));
            label(end);
        }};
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
        return Objects.hash(condition, whenTrue, whenFalse, type);
    }

    @Override
    public Value parsePrecedence(PrecedenceParser state) {
        return parse(state, Value::parsePrecedence);
    }

    @Override
    public Value qualifyNames(NameQualifier state) {
        return parse(state, Value::qualifyNames)
            .withType(type.qualifyNames(state));
    }

    @Override
    public String toString() {
        return stringify(this);
    }

    @Override
    public Conditional withType(Type type) {
        return new Conditional(sourceRange, condition, whenTrue, whenFalse, type);
    }

    private <T> Value parse(T state, BiFunction<Value, T, Value> function) {
        return builder()
            .withSourceRange(sourceRange)
            .withCondition(function.apply(condition, state))
            .withWhenTrue(function.apply(whenTrue, state))
            .withWhenFalse(function.apply(whenFalse, state))
            .withType(type)
            .build();
    }

    public static class Builder implements SyntaxBuilder<Conditional> {

        private Optional<Value>       condition;
        private Optional<Value>       whenTrue;
        private Optional<Value>       whenFalse;
        private Optional<Type>        type;
        private Optional<SourceRange> sourceRange;

        private Builder() {
            condition = Optional.empty();
            whenTrue = Optional.empty();
            whenFalse = Optional.empty();
            type = Optional.empty();
            sourceRange = Optional.empty();
        }

        @Override
        public Conditional build() {
            return conditional(
                require(sourceRange, "Source range"),
                require(condition, "Condition"),
                require(whenTrue, "True case"),
                require(whenFalse, "False case"),
                require(type, "Type")
            );
        }

        public Builder withCondition(Value condition) {
            this.condition = Optional.of(condition);
            return this;
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

        public Builder withWhenFalse(Value whenFalse) {
            this.whenFalse = Optional.of(whenFalse);
            return this;
        }

        public Builder withWhenTrue(Value whenTrue) {
            this.whenTrue = Optional.of(whenTrue);
            return this;
        }
    }
}
