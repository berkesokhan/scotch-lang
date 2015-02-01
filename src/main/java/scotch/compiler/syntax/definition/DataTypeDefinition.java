package scotch.compiler.syntax.definition;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.compiler.syntax.reference.DefinitionReference.dataRef;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.NameQualifier;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.symbol.DataTypeDescriptor;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.text.SourceRange;

public class DataTypeDefinition extends Definition {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceRange                            sourceRange;
    private final Symbol                                 symbol;
    private final List<Type>                             parameters;
    private final Map<Symbol, DataConstructorDefinition> constructors;

    private DataTypeDefinition(SourceRange sourceRange, Symbol symbol, List<Type> parameters, List<DataConstructorDefinition> constructors) {
        this.sourceRange = sourceRange;
        this.symbol = symbol;
        this.parameters = ImmutableList.copyOf(parameters);
        this.constructors = new LinkedHashMap<>();
        constructors.forEach(constructor -> this.constructors.put(constructor.getSymbol(), constructor));
    }

    private DataTypeDefinition(SourceRange sourceRange, Symbol symbol, List<Type> parameters, Map<Symbol, DataConstructorDefinition> constructors) {
        this.sourceRange = sourceRange;
        this.symbol = symbol;
        this.parameters = ImmutableList.copyOf(parameters);
        this.constructors = new LinkedHashMap<>(constructors);
    }

    @Override
    public Definition accumulateDependencies(DependencyAccumulator state) {
        return state.keep(this);
    }

    @Override
    public Definition accumulateNames(NameAccumulator state) {
        return state.scoped(this, () -> {
            state.defineDataType(symbol, getDescriptor());
            constructors.values().forEach(constructor -> constructor.accumulateNames(state));
            return this;
        });
    }

    @Override
    public Definition bindTypes(TypeChecker state) {
        return state.keep(this);
    }

    @Override
    public Definition checkTypes(TypeChecker state) {
        return state.keep(this);
    }

    @Override
    public Definition defineOperators(OperatorAccumulator state) {
        return state.keep(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof DataTypeDefinition) {
            DataTypeDefinition other = (DataTypeDefinition) o;
            return Objects.equals(sourceRange, other.sourceRange)
                && Objects.equals(symbol, other.symbol)
                && Objects.equals(parameters, other.parameters)
                && Objects.equals(constructors, other.constructors);
        } else {
            return false;
        }
    }

    @Override
    public void generateBytecode(BytecodeGenerator state) {
        state.beginClass(symbol.getClassName(), sourceRange);
        state.currentClass().defineDefaultConstructor();
        constructors.values().forEach(constructor -> constructor.generateBytecode(state));
        state.endClass();
    }

    @Override
    public DefinitionReference getReference() {
        return dataRef(symbol);
    }

    @Override
    public SourceRange getSourceRange() {
        return sourceRange;
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, parameters, constructors);
    }

    @Override
    public Optional<Definition> parsePrecedence(PrecedenceParser state) {
        return Optional.of(state.keep(this));
    }

    @Override
    public Definition qualifyNames(NameQualifier state) {
        return state.scoped(this, () -> withParameters(state.qualifyTypeNames(parameters))
            .withConstructors(constructors.values().stream()
                .map(constructor -> constructor.qualifyNames(state))
                .collect(toList())));
    }

    @Override
    public String toString() {
        return symbol.getSimpleName()
            + (parameters.isEmpty() ? "" : " " + parameters.stream().map(Object::toString).collect(joining(", ")))
            + " = " + constructors.values().stream().map(Object::toString).collect(joining(" | "));
    }

    private DataTypeDescriptor getDescriptor() {
        return DataTypeDescriptor.builder(symbol)
            .withParameters(parameters)
            .withConstructors(constructors.values().stream()
                .map(DataConstructorDefinition::getDescriptor)
                .collect(toList()))
            .build();
    }

    private DataTypeDefinition withConstructors(List<DataConstructorDefinition> constructors) {
        return new DataTypeDefinition(sourceRange, symbol, parameters, constructors);
    }

    private DataTypeDefinition withParameters(List<Type> parameters) {
        return new DataTypeDefinition(sourceRange, symbol, parameters, constructors);
    }

    public static class Builder implements SyntaxBuilder<DataTypeDefinition> {

        private Optional<SourceRange>                     sourceRange;
        private Optional<Symbol>                          symbol;
        private List<Type>                         parameters;
        private Optional<List<DataConstructorDefinition>> constructors;

        private Builder() {
            sourceRange = Optional.empty();
            symbol = Optional.empty();
            parameters = new ArrayList<>();
            constructors = Optional.empty();
        }

        public Builder addConstructor(DataConstructorDefinition constructor) {
            if (!constructors.isPresent()) {
                constructors = Optional.of(new ArrayList<>());
            }
            constructors.map(list -> list.add(constructor));
            return this;
        }

        public Builder addParameter(Type type) {
            this.parameters.add(type);
            return this;
        }

        @Override
        public DataTypeDefinition build() {
            return new DataTypeDefinition(
                require(sourceRange, "Source range"),
                require(symbol, "Data type symbol"),
                parameters,
                require(constructors, "No constructors defined")
            );
        }

        public Builder withConstructors(List<DataConstructorDefinition> constructors) {
            constructors.forEach(this::addConstructor);
            return this;
        }

        public Builder withParameters(List<Type> parameters) {
            parameters.forEach(this::addParameter);
            return this;
        }

        @Override
        public Builder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }

        public Builder withSymbol(Symbol symbol) {
            this.symbol = Optional.of(symbol);
            return this;
        }
    }
}
