package scotch.compiler.syntax.value;

import static scotch.util.StringUtil.stringify;

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
import scotch.compiler.syntax.reference.InstanceReference;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.text.SourceRange;

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
    public Value defineOperators(OperatorDefinitionParser state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value parsePrecedence(PrecedenceParser state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof Instance) {
            Instance other = (Instance) o;
            return Objects.equals(sourceRange, other.sourceRange)
                && Objects.equals(reference, other.reference)
                && Objects.equals(type, other.type);
        } else {
            return false;
        }
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        return reference(state.scope());
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
    public int hashCode() {
        return Objects.hash(reference, type);
    }

    @Override
    public Value qualifyNames(NameQualifier state) {
        throw new UnsupportedOperationException();
    }

    public CodeBlock reference(Scope scope) {
        return scope.getTypeInstance(reference.getClassReference(), reference.getModuleReference(), reference.getTypes()).reference();
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + reference + ")";
    }

    @Override
    public Instance withType(Type type) {
        return new Instance(sourceRange, reference, type);
    }
}
