package scotch.compiler.syntax.value;

import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static scotch.util.StringUtil.quote;

import java.util.Objects;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.symbol.Type;
import scotch.compiler.syntax.BytecodeGenerator;
import scotch.compiler.syntax.DependencyAccumulator;
import scotch.compiler.syntax.NameAccumulator;
import scotch.compiler.syntax.NameQualifier;
import scotch.compiler.syntax.OperatorDefinitionParser;
import scotch.compiler.syntax.PrecedenceParser;
import scotch.compiler.syntax.TypeChecker;
import scotch.compiler.text.SourceRange;
import scotch.runtime.Callable;

public abstract class LiteralValue<A> extends Value {

    private final SourceRange sourceRange;
    private final A           value;
    private final Type        type;

    LiteralValue(SourceRange sourceRange, A value, Type type) {
        this.sourceRange = sourceRange;
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
    public Value defineOperators(OperatorDefinitionParser state) {
        return this;
    }

    @Override
    public Value parsePrecedence(PrecedenceParser state) {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof LiteralValue) {
            LiteralValue other = (LiteralValue) o;
            return Objects.equals(sourceRange, other.sourceRange)
                && Objects.equals(value, other.value)
                && Objects.equals(type, other.type);
        } else {
            return false;
        }
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        return loadValue().invokestatic(p(Callable.class), "box", sig(Callable.class, Object.class));
    }

    protected abstract CodeBlock loadValue();

    @Override
    public SourceRange getSourceRange() {
        return sourceRange;
    }

    @Override
    public Type getType() {
        return type;
    }

    public A getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, type);
    }

    @Override
    public Value qualifyNames(NameQualifier state) {
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
