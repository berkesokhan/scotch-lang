package scotch.compiler.syntax.value;

import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static scotch.util.StringUtil.quote;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.text.SourceLocation;
import scotch.runtime.Callable;
import scotch.runtime.RuntimeSupport;
import scotch.symbol.type.Type;

@EqualsAndHashCode(callSuper = false)
public abstract class LiteralValue<A> extends Value {

    @Getter private final SourceLocation sourceLocation;
    @Getter private final A              value;
    @Getter private final Type           type;

    LiteralValue(SourceLocation sourceLocation, A value, Type type) {
        this.sourceLocation = sourceLocation;
        this.value = value;
        this.type = type;
    }

    @Override
    public Value accumulateDependencies(DependencyAccumulator state) {
        return this;
    }

    @Override
    public Value accumulateNames(NameAccumulator state) {
        return this;
    }

    @Override
    public Value bindMethods(TypeChecker state) {
        return this;
    }

    @Override
    public Value checkTypes(TypeChecker state) {
        return this;
    }

    @Override
    public Value bindTypes(TypeChecker state) {
        return this;
    }

    @Override
    public Value defineOperators(OperatorAccumulator state) {
        return this;
    }

    @Override
    public Value parsePrecedence(PrecedenceParser state) {
        return this;
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        return loadValue().invokestatic(p(RuntimeSupport.class), "box", sig(Callable.class, Object.class));
    }

    protected abstract CodeBlock loadValue();

    @Override
    public Value qualifyNames(ScopedNameQualifier state) {
        return this;
    }

    @Override
    public String toString() {
        return "(" + quote(value) + " :: " + type + ")";
    }

    @Override
    public Value withType(Type type) {
        return this;
    }
}
