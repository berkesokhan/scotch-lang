package scotch.compiler.syntax.value;

import static java.util.stream.Collectors.toList;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.NameQualifier;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.syntax.reference.InstanceReference;
import scotch.compiler.text.SourceRange;

@EqualsAndHashCode(callSuper = false)
@ToString
public class Instance extends Value {

    private final SourceRange       sourceRange;
    private final InstanceReference reference;
    private final Type              type;

    Instance(SourceRange sourceRange, InstanceReference reference, Type type) {
        this.sourceRange = sourceRange;
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
                .map(parameter -> parameter.reify(state.scope()))
                .collect(toList())
        ).reference();
    }

    @Override
    public SourceRange getSourceRange() {
        return sourceRange;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Value qualifyNames(NameQualifier state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Instance withType(Type type) {
        return new Instance(sourceRange, reference, type);
    }
}
