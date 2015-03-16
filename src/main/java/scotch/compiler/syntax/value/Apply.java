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
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.symbol.type.FunctionType;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.symbol.type.Unification;
import scotch.compiler.text.SourceRange;
import scotch.runtime.Applicable;
import scotch.runtime.Callable;
import scotch.runtime.SuppliedThunk;

@AllArgsConstructor(access = PACKAGE)
@EqualsAndHashCode(callSuper = false)
@ToString(of = { "type", "function", "argument" })
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
    public Value bindTypes(TypeChecker state) {
        return new Apply(sourceRange, function.bindTypes(state), argument.bindTypes(state), state.generate(type));
    }

    @Override
    public Value bindMethods(TypeChecker state, InstanceMap instances) {
        return withFunction(function.bindMethods(state, instances))
            .withArgument(argument.bindMethods(state, instances));
    }

    @Override
    public Value checkTypes(TypeChecker state) {
        Value checkedFunction = function.checkTypes(state);
        Value checkedArgument = argument.checkTypes(state);
        Unification unify = fn(checkedArgument.getType(), type)
            .unify(checkedFunction.getType(), state.scope());
        return new Apply(sourceRange, checkedFunction, checkedArgument, unify
                .mapType(t -> ((FunctionType) t).getResult())
                .orElseGet(unification -> {
                    state.error(typeError(unification.flip(), checkedArgument.getSourceRange()));
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
    public SourceRange getSourceRange() {
        return sourceRange;
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
