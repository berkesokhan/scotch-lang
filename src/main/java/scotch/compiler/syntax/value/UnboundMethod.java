package scotch.compiler.syntax.value;

import static java.util.stream.Collectors.toList;
import static scotch.util.StringUtil.stringify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.symbol.NameQualifier;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.syntax.BytecodeGenerator;
import scotch.compiler.syntax.DependencyAccumulator;
import scotch.compiler.syntax.NameAccumulator;
import scotch.compiler.syntax.OperatorDefinitionParser;
import scotch.compiler.syntax.PrecedenceParser;
import scotch.compiler.syntax.TypeChecker;
import scotch.compiler.syntax.reference.ValueReference;
import scotch.compiler.syntax.scope.Scope;
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

    public Value bind(Scope scope) {
        List<Type> instances = listInstanceTypes(scope.getRawValue(valueRef));
        List<Type> instanceTypes = listInstanceTypes(scope.getValue(valueRef));
        return method(sourceRange, valueRef, instances, scope.generate(getMethodType(instanceTypes)));
    }

    @Override
    public Value bindMethods(TypeChecker state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value bindTypes(TypeChecker state) {
        return bind(state.scope()).bindTypes(state);
    }

    @Override
    public Value checkTypes(TypeChecker state) {
        return this;
    }

    @Override
    public Value defineOperators(OperatorDefinitionParser state) {
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
        throw new UnsupportedOperationException();
    }

    public Type getMethodType(List<Type> instances) {
        List<Type> reversedInstances = new ArrayList<>(instances);
        Collections.reverse(reversedInstances);
        return reversedInstances.stream().reduce(type, (left, right) -> Type.fn(right, left));
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

    public List<Type> listInstanceTypes(Type valueType) {
        return valueType.getContexts().stream()
            .map(tuple -> tuple.into((type, symbol) -> Type.instance(symbol, type.simplify())))
            .collect(toList());
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
}
