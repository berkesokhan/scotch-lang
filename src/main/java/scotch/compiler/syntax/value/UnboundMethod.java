package scotch.compiler.syntax.value;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.error.SymbolNotFoundError.symbolNotFound;
import static scotch.symbol.type.Types.fn;
import static scotch.symbol.type.Types.instance;
import static scotch.compiler.syntax.value.NoBindingError.noBinding;
import static scotch.compiler.syntax.value.Values.method;
import static scotch.compiler.syntax.value.Values.unboundMethod;
import static scotch.util.StringUtil.stringify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.intermediate.IntermediateGenerator;
import scotch.compiler.intermediate.IntermediateValue;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.symbol.Symbol;
import scotch.symbol.type.InstanceType;
import scotch.symbol.type.Type;
import scotch.compiler.syntax.reference.ValueReference;
import scotch.compiler.text.SourceLocation;

public class UnboundMethod extends Value {

    private final SourceLocation sourceLocation;
    private final ValueReference valueRef;
    private final Type           type;

    UnboundMethod(SourceLocation sourceLocation, ValueReference valueRef, Type type) {
        this.sourceLocation = sourceLocation;
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
    public IntermediateValue generateIntermediateCode(IntermediateGenerator state) {
        throw new UnsupportedOperationException(); // TODO
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
            return Objects.equals(sourceLocation, other.sourceLocation)
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
    public SourceLocation getSourceLocation() {
        return sourceLocation;
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
    public Value qualifyNames(ScopedNameQualifier state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + valueRef.getName() + ")";
    }

    @Override
    public Value withType(Type type) {
        return unboundMethod(sourceLocation, valueRef, type);
    }

    private Value bind(TypeChecker state) {
        return state.getRawValue(valueRef)
            .map(valueType -> {
                List<InstanceType> instances = listInstanceTypes(valueType);
                return state.getRawValue(valueRef)
                    .map(rawValue -> rawValue.zip(type, state)
                        .map(map -> instances.stream()
                            .map(instance -> instance.withBinding(map.get(instance.getBinding())))
                            .collect(toList()))
                        .map(instanceTypes -> method(sourceLocation, valueRef, instances, state.generate(getMethodType(instanceTypes))))
                        .orElseGet(() -> {
                            state.error(noBinding(getSymbol(), sourceLocation));
                            return this;
                        }))
                    .orElseGet(() -> notFound(state));
            })
            .orElseGet(() -> notFound(state));
    }

    private UnboundMethod notFound(TypeChecker state) {
        state.error(symbolNotFound(valueRef.getSymbol(), sourceLocation));
        return this;
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
