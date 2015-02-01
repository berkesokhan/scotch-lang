package scotch.compiler.syntax.value;

import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static scotch.compiler.syntax.TypeError.typeError;
import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import java.util.function.Supplier;
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
import scotch.compiler.symbol.Unification;
import scotch.compiler.symbol.Unification.UnificationVisitor;
import scotch.compiler.symbol.Unification.Unified;
import scotch.compiler.text.SourceRange;
import scotch.runtime.Applicable;
import scotch.runtime.Callable;
import scotch.runtime.SuppliedThunk;

public class Apply extends Value {

    private final SourceRange sourceRange;
    private final Value       function;
    private final Value       argument;
    private final Type        type;

    Apply(SourceRange sourceRange, Value function, Value argument, Type type) {
        this.sourceRange = sourceRange;
        this.function = function;
        this.argument = argument;
        this.type = type;
    }

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
        Type resultType = state.reserveType();
        return function.getType().unify(Type.fn(argument.getType(), resultType), state.scope()).accept(new UnificationVisitor<Value>() {
            @Override
            public Value visit(Unified unified) {
                Value typedFunction = function.withType(state.generate(function.getType()));
                Value typedArgument = argument.withType(state.generate(argument.getType()));
                return withFunction(typedFunction)
                    .withArgument(typedArgument)
                    .withType(state.generate(resultType));
            }

            @Override
            public Value visitOtherwise(Unification unification) {
                state.error(typeError(unification, sourceRange));
                return withType(resultType);
            }
        });
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
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof Apply) {
            Apply other = (Apply) o;
            return Objects.equals(function, other.function)
                && Objects.equals(argument, other.argument)
                && Objects.equals(type, other.type);
        } else {
            return false;
        }
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
    public int hashCode() {
        return Objects.hash(function, argument, type);
    }

    @Override
    public Value qualifyNames(NameQualifier state) {
        return withFunction(function.qualifyNames(state)).withArgument(argument.qualifyNames(state));
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + function + ", " + argument + ")";
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
