package scotch.compiler.syntax.definition;

import static scotch.compiler.syntax.reference.DefinitionReference.classRef;
import static scotch.util.StringUtil.stringify;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.text.SourceRange;

public class ClassDefinition extends Definition {

    private final SourceRange               sourceRange;
    private final Symbol                    symbol;
    private final List<Type>                arguments;
    private final List<DefinitionReference> members;

    ClassDefinition(SourceRange sourceRange, Symbol symbol, List<Type> arguments, List<DefinitionReference> members) {
        if (arguments.isEmpty()) {
            throw new IllegalArgumentException("Can't create class definition with 0 arguments");
        } else if (members.isEmpty()) {
            throw new IllegalArgumentException("Can't create class definition with 0 members");
        }
        this.sourceRange = sourceRange;
        this.symbol = symbol;
        this.arguments = ImmutableList.copyOf(arguments);
        this.members = ImmutableList.copyOf(members);
    }

    @Override
    public Definition accumulateDependencies(DependencyAccumulator state) {
        return state.keep(this);
    }

    @Override
    public Definition accumulateNames(NameAccumulator state) {
        return state.keep(this);
    }

    @Override
    public Definition checkTypes(TypeChecker state) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public Definition defineOperators(OperatorAccumulator state) {
        return state.keep(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof ClassDefinition) {
            ClassDefinition other = (ClassDefinition) o;
            return Objects.equals(symbol, other.symbol)
                && Objects.equals(arguments, other.arguments)
                && Objects.equals(members, other.members);
        } else {
            return false;
        }
    }

    @Override
    public void generateBytecode(BytecodeGenerator state) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public DefinitionReference getReference() {
        return classRef(symbol);
    }

    @Override
    public SourceRange getSourceRange() {
        return sourceRange;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, arguments, members);
    }

    @Override
    public Optional<Definition> parsePrecedence(PrecedenceParser state) {
        return Optional.of(state.keep(this));
    }

    @Override
    public Definition qualifyNames(ScopedNameQualifier state) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + symbol + ")";
    }
}
