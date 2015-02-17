package scotch.compiler.syntax.value;

import static java.util.stream.Collectors.joining;
import static scotch.util.StringUtil.stringify;

import java.util.List;
import java.util.Objects;
import com.google.common.collect.ImmutableList;
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

public class Method extends Value {

    private final SourceRange    sourceRange;
    private final ValueReference reference;
    private final List<Type>     instances;
    private final Type           type;

    Method(SourceRange sourceRange, ValueReference reference, List<? extends Type> instances, Type type) {
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
        throw new UnsupportedOperationException(); // TODO
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
    public Value defineOperators(OperatorAccumulator state) {
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
        return state.getValueSignature(reference.getSymbol()).reference();
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

    @Override
    public String toString() {
        return stringify(this) + "(" + reference.getMemberName() + " [" + instances.stream().map(Object::toString).collect(joining(", ")) + "])";
    }

    @Override
    public Method withType(Type type) {
        return new Method(sourceRange, reference, instances, type);
    }
}
