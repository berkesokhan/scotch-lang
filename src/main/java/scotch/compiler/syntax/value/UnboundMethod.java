package scotch.compiler.syntax.value;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.symbol.type.Types.fn;
import static scotch.compiler.symbol.type.Types.instance;
import static scotch.compiler.syntax.value.NoBindingError.noBinding;
import static scotch.compiler.syntax.value.Values.method;
import static scotch.compiler.syntax.value.Values.unboundMethod;
import static scotch.util.StringUtil.stringify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import scotch.compiler.symbol.type.InstanceType;
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
        List<InstanceType> instances = listInstanceTypes(state.getRawValue(valueRef));
        return state.getRawValue(valueRef)
            .zip(type, state)
            .map(map -> instances.stream()
                .map(instance -> instance.withBinding(map.get(instance.getBinding())))
                .collect(toList()))
            .map(instanceTypes -> method(sourceRange, valueRef, instances, state.generate(getMethodType(instanceTypes))))
            .orElseGet(() -> {
                state.error(noBinding(getSymbol(), sourceRange));
                return this;
            });
    }

    private Type getMethodType(List<InstanceType> instances) {
        List<Type> reversedInstances = new ArrayList<>(instances);
        Collections.reverse(reversedInstances);
        return reversedInstances.stream().reduce(type, (left, right) -> fn(right, left));
    }

    private List<InstanceType> listInstanceTypes(Type valueType) {
        return valueType.getContexts().stream()
            .map(pair -> pair.into((type, symbol) -> instance(symbol, type.simplify())))
            .collect(toList());
    }
}
