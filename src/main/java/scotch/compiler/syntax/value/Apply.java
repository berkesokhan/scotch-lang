package scotch.compiler.syntax.value;

import static lombok.AccessLevel.PACKAGE;
import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static scotch.compiler.symbol.type.Types.fn;
import static scotch.compiler.syntax.TypeError.typeError;

import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import me.qmx.jitescript.CodeBlock;
import me.qmx.jitescript.LambdaBlock;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.NameQualifier;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.symbol.type.Unification;
import scotch.compiler.text.SourceRange;
import scotch.runtime.Applicable;
import scotch.runtime.Callable;
import scotch.runtime.SuppliedThunk;

@AllArgsConstructor(access = PACKAGE)
@EqualsAndHashCode(callSuper = false)
@ToString
public class Apply extends Value {

    private final SourceRange sourceRange;
    private final Value       function;
    private final Value       argument;
    private final Type        type;

    @Override
    public Value accumulateDependencies(DependencyAccumulator state) {
        return withFunction(function.accumulateDependencies(state)).withArgument(argument.accumulateDependencies(state));
    }

    @Override
    public Value accumulateNames(NameAccumulator state) {
        return withFunction(function.accumulateNames(state))
            .withArgument(argument.accumulateNames(state));
    }

    @Override
    public Value bindMethods(TypeChecker state) {
        return withFunction(function.bindMethods(state))
            .withArgument(argument.bindMethods(state));
    }

    @Override
    public Value checkTypes(TypeChecker state) {
        Value function = this.function.checkTypes(state);
        Value argument = this.argument.checkTypes(state);
        Type result = state.reserveType();
        Unification unification = fn(argument.getType(), result).unify(function.getType(), state.scope());
        if (unification.isUnified()) {
            Value typedFunction = function.withType(state.generate(function.getType()));
            Value typedArgument = argument.withType(state.generate(argument.getType()));
            Type typedResult = state.generate(result);
            Unification typedUnification = fn(typedArgument.getType(), typedResult).unify(typedFunction.getType(), state.scope());
            if (typedUnification.isUnified()) {
                Type argumentType = state.generate(typedArgument.getType());
                Type resultType = state.generate(typedResult);
                Type functionType = fn(argumentType, resultType);
                return withFunction(typedFunction.withType(functionType))
                    .withArgument(typedArgument.withType(argumentType))
                    .withType(resultType);
            } else {
                state.error(typeError(typedUnification.flip(), argument.getSourceRange()));
                return withType(typedResult);
            }
        } else {
            state.error(typeError(unification.flip(), argument.getSourceRange()));
            return withType(result);
        }
    }

    @Override
    public Value bindTypes(TypeChecker state) {
        return withFunction(function.bindTypes(state))
            .withArgument(argument.bindTypes(state))
            .withType(state.generate(type));
    }

    @Override
    public Value defineOperators(OperatorAccumulator state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value parsePrecedence(PrecedenceParser state) {
        return withFunction(function.parsePrecedence(state))
            .withArgument(argument.parsePrecedence(state));
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        return new CodeBlock() {{
            newobj(p(SuppliedThunk.class));
            dup();
            append(state.captureApply());
            lambda(state.currentClass(), new LambdaBlock(state.reserveApply()) {{
                function(p(Supplier.class), "get", sig(Object.class));
                specialize(sig(Callable.class));
                capture(state.getCaptureAllTypes());
                Class<?> returnType = state.typeOf(type);
                delegateTo(ACC_STATIC, sig(returnType, state.getCaptureAllTypes()), new CodeBlock() {{
                    append(function.generateBytecode(state));
                    invokeinterface(p(Callable.class), "call", sig(Object.class));
                    checkcast(p(Applicable.class));
                    append(argument.generateBytecode(state));
                    invokeinterface(p(Applicable.class), "apply", sig(Callable.class, Callable.class));
                    if (returnType != Callable.class) {
                        checkcast(p(returnType));
                    }
                    areturn();
                }});
            }});
            invokespecial(p(SuppliedThunk.class), "<init>", sig(void.class, Supplier.class));
        }};
    }

    public Value getArgument() {
        return argument;
    }

    public Value getFunction() {
        return function;
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
    public Value qualifyNames(NameQualifier state) {
        return withFunction(function.qualifyNames(state)).withArgument(argument.qualifyNames(state));
    }

    public Apply withArgument(Value argument) {
        return new Apply(sourceRange, function, argument, type);
    }

    public Apply withFunction(Value function) {
        return new Apply(sourceRange, function, argument, type);
    }

    public Apply withSourceRange(SourceRange sourceRange) {
        return new Apply(sourceRange, function, argument, type);
    }

    @Override
    public Apply withType(Type type) {
        return new Apply(sourceRange, function, argument, type);
    }
}
