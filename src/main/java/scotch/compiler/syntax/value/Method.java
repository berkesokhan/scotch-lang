package scotch.compiler.syntax.value;

import static java.util.stream.Collectors.joining;
import static scotch.util.StringUtil.stringify;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.google.common.collect.ImmutableList;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.Type.FunctionType;
import scotch.compiler.symbol.Type.InstanceType;
import scotch.compiler.syntax.BytecodeGenerator;
import scotch.compiler.syntax.DependencyAccumulator;
import scotch.compiler.syntax.NameAccumulator;
import scotch.compiler.syntax.NameQualifier;
import scotch.compiler.syntax.OperatorDefinitionParser;
import scotch.compiler.syntax.PrecedenceParser;
import scotch.compiler.syntax.TypeChecker;
import scotch.compiler.syntax.reference.ValueReference;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.text.SourceRange;

public class Method extends Value {

    private final SourceRange    sourceRange;
    private final ValueReference reference;
    private final List<Type>     instances;
    private final Type           type;

    Method(SourceRange sourceRange, ValueReference reference, List<Type> instances, Type type) {
        this.sourceRange = sourceRange;
        this.reference = reference;
        this.instances = ImmutableList.copyOf(instances);
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
        List<InstanceType> instanceTypes = new ArrayList<>();
        Type type = this.type;
        for (int i = 0; i < instances.size(); i++) {
            instanceTypes.add((InstanceType) ((FunctionType) type).getArgument());
            type = ((FunctionType) type).getResult();
        }
        Value result = this;
        for (InstanceType instanceType : instanceTypes) {
            Value typeArgument;
            if (instanceType.isBound()) {
                typeArgument = state.findInstance(this, instanceType);
            } else {
                typeArgument = state.findArgument(instanceType);
            }
            result = apply(result, typeArgument, ((FunctionType) result.getType()).getResult());
        }
        return result;
    }

    @Override
    public Value bindTypes(TypeChecker state) {
        return withType(state.generate(type));
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
        } else if (o instanceof Method) {
            Method other = (Method) o;
            return Objects.equals(sourceRange, other.sourceRange)
                && Objects.equals(reference, other.reference)
                && Objects.equals(instances, other.instances)
                && Objects.equals(type, other.type);
        } else {
            return false;
        }
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        return reference(state.scope());
    }

    public int getInstanceCount() {
        return instances.size();
    }

    public ValueReference getReference() {
        return reference;
    }

    @Override
    public SourceRange getSourceRange() {
        return sourceRange;
    }

    public Symbol getSymbol() {
        return reference.getSymbol();
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(reference, instances, type);
    }

    @Override
    public Value parsePrecedence(PrecedenceParser state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value qualifyNames(NameQualifier state) {
        throw new UnsupportedOperationException();
    }

    public CodeBlock reference(Scope scope) {
        return scope.getValueSignature(reference.getSymbol()).get().reference();
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + reference.getMemberName() + " [" + instances.stream().map(Object::toString).collect(joining(", ")) + "])";
    }

    @Override
    public Method withType(Type type) {
        return new Method(sourceRange, reference, instances, type);
    }
}
