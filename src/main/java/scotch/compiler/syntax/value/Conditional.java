package scotch.compiler.syntax.value;

import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static scotch.compiler.syntax.TypeError.typeError;
import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.compiler.syntax.value.Values.conditional;
import static scotch.symbol.type.Types.sum;

import java.util.Optional;
import java.util.function.BiFunction;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import me.qmx.jitescript.CodeBlock;
import org.objectweb.asm.tree.LabelNode;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.text.SourceRange;
import scotch.runtime.Callable;
import scotch.runtime.RuntimeSupport;
import scotch.symbol.type.Type;

@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "sourceRange")
public class Conditional extends Value {

    public static Builder builder() {
        return new Builder();
    }

    @Getter
    private final SourceRange sourceRange;
    @Getter
    private final Type        type;
    private final Value       condition;
    private final Value       whenTrue;
    private final Value       whenFalse;

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
    public Value accumulateNames(NameAccumulator state) {
        return parse(state, Value::accumulateNames);
    }

    @Override
    public Value bindMethods(TypeChecker state) {
        return parse(state, (value, s) -> value.bindMethods(s));
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
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        return new CodeBlock() {{
            LabelNode falseBranch = new LabelNode();
            LabelNode end = new LabelNode();
            append(condition.generateBytecode(state));
            invokestatic(p(RuntimeSupport.class), "unboxBool", sig(boolean.class, Callable.class));
            iffalse(falseBranch);
            append(whenTrue.generateBytecode(state));
            go_to(end);
            label(falseBranch);
            append(whenFalse.generateBytecode(state));
            label(end);
        }};
    }

    @Override
    public Value parsePrecedence(PrecedenceParser state) {
        return parse(state, Value::parsePrecedence);
    }

    @Override
    public Value qualifyNames(ScopedNameQualifier state) {
        return parse(state, Value::qualifyNames)
            .withType(type.qualifyNames(state));
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
