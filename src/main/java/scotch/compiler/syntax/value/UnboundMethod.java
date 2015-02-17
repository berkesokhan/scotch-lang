package scotch.compiler.syntax.value;

import static scotch.compiler.syntax.value.Values.unboundMethod;
import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.NameQualifier;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.syntax.reference.ValueReference;
import scotch.compiler.text.SourceRange;

public class UnboundMethod extends Value {

    private final SourceRange    sourceRange;
    private final ValueReference valueRef;
    private final Type           type;

    UnboundMethod(SourceRange sourceRange, ValueReference valueRef, Type type) {
        this.sourceRange = sourceRange;
        this.valueRef = valueRef;
        this.type = type;
    }

    @Override
    public Value accumulateDependencies(DependencyAccumulator state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value accumulateNames(NameAccumulator state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value bindMethods(TypeChecker state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value bindTypes(TypeChecker state) {
        return bind(state).bindTypes(state);
    }

    @Override
    public Value checkTypes(TypeChecker state) {
        return this;
    }

    @Override
    public Value defineOperators(OperatorAccumulator state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof UnboundMethod) {
            UnboundMethod other = (UnboundMethod) o;
            return Objects.equals(sourceRange, other.sourceRange)
                && Objects.equals(valueRef, other.valueRef)
                && Objects.equals(type, other.type);
        } else {
            return false;
        }
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        throw new IllegalStateException("Can't generate bytecode for unbound method");
    }

    @Override
    public SourceRange getSourceRange() {
        return sourceRange;
    }

    public Symbol getSymbol() {
        return valueRef.getSymbol();
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(valueRef, type);
    }

    @Override
    public Value parsePrecedence(PrecedenceParser state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value qualifyNames(NameQualifier state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + valueRef.getName() + ")";
    }

    @Override
    public Value withType(Type type) {
        return unboundMethod(sourceRange, valueRef, type);
    }

    private Value bind(TypeChecker state) {
        throw new UnsupportedOperationException(); // TODO
    }
}
