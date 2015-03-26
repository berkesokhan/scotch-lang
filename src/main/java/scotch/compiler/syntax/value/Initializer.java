package scotch.compiler.syntax.value;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.builder.BuilderUtil.require;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import lombok.EqualsAndHashCode;
import lombok.ToString;
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
import scotch.symbol.type.Type;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.text.SourceLocation;

@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "sourceLocation")
public class Initializer extends Value {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceLocation         sourceLocation;
    private final Value                  value;
    private final List<InitializerField> fields;
    private final Type                   type;

    Initializer(SourceLocation sourceLocation, Value value, List<InitializerField> fields, Type type) {
        this.sourceLocation = sourceLocation;
        this.value = value;
        this.fields = ImmutableList.copyOf(fields);
        this.type = type;
    }

    @Override
    public Value accumulateDependencies(DependencyAccumulator state) {
        return new Initializer(
            sourceLocation,
            value.accumulateDependencies(state),
            fields.stream()
                .map(field -> field.accumulateDependencies(state))
                .collect(toList()),
            type
        );
    }

    @Override
    public Value accumulateNames(NameAccumulator state) {
        return new Initializer(
            sourceLocation,
            value.accumulateNames(state),
            fields.stream()
                .map(field -> field.accumulateNames(state))
                .collect(toList()),
            type
        );
    }

    @Override
    public IntermediateValue generateIntermediateCode(IntermediateGenerator state) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public Value bindMethods(TypeChecker state) {
        return this;
    }

    @Override
    public Value bindTypes(TypeChecker state) {
        return this;
    }

    @Override
    public Value checkTypes(TypeChecker state) {
        return value
            .asInitializer(this, state)
            .orElse(this);
    }

    @Override
    public Value defineOperators(OperatorAccumulator state) {
        return new Initializer(
            sourceLocation,
            value.defineOperators(state),
            fields.stream()
                .map(field -> field.defineOperators(state))
                .collect(toList()),
            type
        );
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        throw new UnsupportedOperationException();
    }

    public List<InitializerField> getFields() {
        return fields;
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
    public Value parsePrecedence(PrecedenceParser state) {
        return new Initializer(
            sourceLocation,
            value.parsePrecedence(state),
            fields.stream()
                .map(field -> field.parsePrecedence(state))
                .collect(toList()),
            type
        );
    }

    @Override
    public Value qualifyNames(ScopedNameQualifier state) {
        return withValue(value.qualifyNames(state))
            .withFields(fields.stream()
                .map(field -> field.qualifyNames(state))
                .collect(toList()));
    }

    @Override
    public Value withType(Type type) {
        return new Initializer(sourceLocation, value, fields, type);
    }

    private Value withFields(List<InitializerField> fields) {
        return new Initializer(sourceLocation, value, fields, type);
    }

    private Initializer withValue(Value value) {
        return new Initializer(sourceLocation, value, fields, type);
    }

    public static class Builder implements SyntaxBuilder<Initializer> {

        private Optional<SourceLocation>         sourceLocation;
        private Optional<Value>                  value;
        private Optional<List<InitializerField>> fields;
        private Optional<Type>                   type;

        private Builder() {
            sourceLocation = Optional.empty();
            value = Optional.empty();
            fields = Optional.empty();
            type = Optional.empty();
        }

        public Builder addField(InitializerField field) {
            if (!fields.isPresent()) {
                fields = Optional.of(new ArrayList<>());
            }
            fields.ifPresent(list -> list.add(field));
            return this;
        }

        @Override
        public Initializer build() {
            return new Initializer(
                require(sourceLocation, "Source location"),
                require(value, "Initializer value"),
                require(fields, "Initializer fields"),
                require(type, "Initializer type")
            );
        }

        @Override
        public Builder withSourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = Optional.of(sourceLocation);
            return this;
        }

        public Builder withType(Type type) {
            this.type = Optional.of(type);
            return this;
        }

        public Builder withValue(Value value) {
            this.value = Optional.of(value);
            return this;
        }
    }
}
