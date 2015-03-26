package scotch.compiler.syntax.value;

import static lombok.AccessLevel.PACKAGE;
import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static scotch.compiler.intermediate.Intermediates.apply;
import static scotch.compiler.syntax.TypeError.typeError;
import static scotch.symbol.type.Types.fn;

import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import me.qmx.jitescript.CodeBlock;
import me.qmx.jitescript.LambdaBlock;
import scotch.compiler.intermediate.IntermediateGenerator;
import scotch.compiler.intermediate.IntermediateValue;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.text.SourceLocation;
import scotch.runtime.Applicable;
import scotch.runtime.Callable;
import scotch.runtime.SuppliedThunk;
import scotch.symbol.type.FunctionType;
import scotch.symbol.type.Type;
import scotch.symbol.type.Unification;

@AllArgsConstructor(access = PACKAGE)
@EqualsAndHashCode(callSuper = false)
@ToString(of = { "type", "function", "argument" })
public class Apply extends Value {

    private final SourceLocation sourceLocation;
    private final Value          function;
    private final Value          argument;
    private final Type           type;

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
    public IntermediateValue generateIntermediateCode(IntermediateGenerator state) {
        return apply(state.getCaptures(), function.generateIntermediateCode(state), argument.generateIntermediateCode(state));
    }

    @Override
    public Value bindTypes(TypeChecker state) {
        return new Apply(sourceLocation, function.bindTypes(state), argument.bindTypes(state), state.generate(type));
    }

    @Override
    public Value bindMethods(TypeChecker state) {
        return withFunction(function.bindMethods(state))
            .withArgument(argument.bindMethods(state));
    }

    @Override
    public Value checkTypes(TypeChecker state) {
        Value checkedFunction = function.checkTypes(state);
        Value checkedArgument = argument.checkTypes(state);
        Unification unify = fn(checkedArgument.getType(), type)
            .unify(checkedFunction.getType(), state.scope());
        return new Apply(sourceLocation, checkedFunction, checkedArgument, unify
            .mapType(t -> ((FunctionType) t).getResult())
            .orElseGet(unification -> {
                    state.error(typeError(unification.flip(), checkedArgument.getSourceLocation()));
                    return type;
                }));
    }

    @Override
    public Value defineOperators(OperatorAccumulator state) {
        throw new UnsupportedOperationException();
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
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Value parsePrecedence(PrecedenceParser state) {
        return withFunction(function.parsePrecedence(state))
            .withArgument(argument.parsePrecedence(state));
    }

    @Override
    public Value qualifyNames(ScopedNameQualifier state) {
        return withFunction(function.qualifyNames(state)).withArgument(argument.qualifyNames(state));
    }

    public Apply withArgument(Value argument) {
        return new Apply(sourceLocation, function, argument, type);
    }

    public Apply withFunction(Value function) {
        return new Apply(sourceLocation, function, argument, type);
    }

    public Apply withSourceLocation(SourceLocation sourceLocation) {
        return new Apply(sourceLocation, function, argument, type);
    }

    @Override
    public Apply withType(Type type) {
        return new Apply(sourceLocation, function, argument, type);
    }
}
