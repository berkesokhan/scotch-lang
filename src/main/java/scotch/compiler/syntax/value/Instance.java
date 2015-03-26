package scotch.compiler.syntax.value;

import static java.util.stream.Collectors.toList;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.intermediate.IntermediateGenerator;
import scotch.compiler.intermediate.IntermediateValue;
import scotch.compiler.intermediate.Intermediates;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.symbol.type.Type;
import scotch.compiler.syntax.reference.InstanceReference;
import scotch.compiler.text.SourceLocation;

@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "sourceLocation")
public class Instance extends Value {

    private final SourceLocation    sourceLocation;
    private final InstanceReference reference;
    private final Type              type;

    Instance(SourceLocation sourceLocation, InstanceReference reference, Type type) {
        this.sourceLocation = sourceLocation;
        this.reference = reference;
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
        return Intermediates.instanceRef(reference);
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
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        return state.getTypeInstance(
            reference.getClassReference(),
            reference.getModuleReference(),
            reference.getParameters().stream()
                .map(parameter -> parameter.copy(state.scope()::reserveType))
                .collect(toList())
        ).reference();
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Value qualifyNames(ScopedNameQualifier state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Instance withType(Type type) {
        return new Instance(sourceLocation, reference, type);
    }
}
