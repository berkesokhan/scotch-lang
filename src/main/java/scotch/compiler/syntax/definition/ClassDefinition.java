package scotch.compiler.syntax.definition;

import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.compiler.syntax.definition.Definitions.classDef;
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
import scotch.symbol.Symbol;
import scotch.symbol.type.Type;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.text.SourceLocation;

public class ClassDefinition extends Definition {

    private final SourceLocation            sourceLocation;
    private final Symbol                    symbol;
    private final List<Type>                arguments;
    private final List<DefinitionReference> members;

    ClassDefinition(SourceLocation sourceLocation, Symbol symbol, List<Type> arguments, List<DefinitionReference> members) {
        if (arguments.isEmpty()) {
            throw new IllegalArgumentException("Can't create class definition with 0 arguments");
        } else if (members.isEmpty()) {
            throw new IllegalArgumentException("Can't create class definition with 0 members");
        }
        this.sourceLocation = sourceLocation;
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
    public SourceLocation getSourceLocation() {
        return sourceLocation;
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

    public static class ClassDefinitionBuilder implements SyntaxBuilder<ClassDefinition> {

        private Optional<Symbol>                    symbol;
        private Optional<List<Type>>                arguments;
        private Optional<List<DefinitionReference>> members;
        private Optional<SourceLocation>            sourceLocation;

        public ClassDefinitionBuilder() {
            symbol = Optional.empty();
            arguments = Optional.empty();
            members = Optional.empty();
            sourceLocation = Optional.empty();
        }

        @Override
        public ClassDefinition build() {
            return classDef(
                require(sourceLocation, "Source location"),
                require(symbol, "Class symbol"),
                require(arguments, "Class arguments"),
                require(members, "Class member definitions")
            );
        }

        public ClassDefinitionBuilder withArguments(List<Type> arguments) {
            this.arguments = Optional.of(arguments);
            return this;
        }

        public ClassDefinitionBuilder withMembers(List<DefinitionReference> members) {
            this.members = Optional.of(members);
            return this;
        }

        @Override
        public ClassDefinitionBuilder withSourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = Optional.of(sourceLocation);
            return this;
        }

        public ClassDefinitionBuilder withSymbol(Symbol symbol) {
            this.symbol = Optional.of(symbol);
            return this;
        }
    }
}
