package scotch.compiler.syntax.value;

import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import me.qmx.jitescript.CodeBlock;
import me.qmx.jitescript.LambdaBlock;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulatorState;
import scotch.compiler.steps.NameQualifier;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.text.SourceRange;
import scotch.runtime.Applicable;
import scotch.runtime.Callable;

public class LambdaValue extends Value {

    private final Argument argument;
    private final Value body;

    LambdaValue(Argument argument, Value body) {
        this.argument = argument;
        this.body = body;
    }

    @Override
    public Value accumulateDependencies(DependencyAccumulator state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value accumulateNames(NameAccumulatorState state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value bindMethods(TypeChecker state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value checkTypes(TypeChecker state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value bindTypes(TypeChecker state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value defineOperators(OperatorAccumulator state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value parsePrecedence(PrecedenceParser state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof LambdaValue) {
            LambdaValue other = (LambdaValue) o;
            return Objects.equals(argument, other.argument)
                && Objects.equals(body, other.body);
        } else {
            return false;
        }
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        return new CodeBlock() {{
            append(state.captureLambda(argument.getName()));
            lambda(state.currentClass(), new LambdaBlock(state.reserveLambda()) {{
                function(p(Applicable.class), "apply", sig(Callable.class, Callable.class));
                capture(state.getLambdaCaptureTypes());
                delegateTo(ACC_STATIC, sig(state.typeOf(body.getType()), state.getLambdaType()), new CodeBlock() {{
                    append(body.generateBytecode(state));
                    areturn();
                }});
            }});
            state.releaseLambda(argument.getName());
        }};
    }

    public Value getBody() {
        return body;
    }

    @Override
    public SourceRange getSourceRange() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Type getType() {
        return Type.fn(argument.getType(), body.getType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(argument, body);
    }

    @Override
    public Value qualifyNames(NameQualifier state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return stringify(this);
    }

    @Override
    public Value withType(Type type) {
        throw new UnsupportedOperationException();
    }
}
