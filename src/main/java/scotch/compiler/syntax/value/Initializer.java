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
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.text.SourceRange;

@EqualsAndHashCode(callSuper = false)
@ToString
public class Initializer extends Value {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceRange            sourceRange;
    private final Value                  value;
    private final List<InitializerField> fields;
    private final Type                   type;

    Initializer(SourceRange sourceRange, Value value, List<InitializerField> fields, Type type) {
        this.sourceRange = sourceRange;
        this.value = value;
        this.fields = ImmutableList.copyOf(fields);
        this.type = type;
    }

    @Override
    public Value accumulateDependencies(DependencyAccumulator state) {
        return new Initializer(
            sourceRange,
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
            sourceRange,
            value.accumulateNames(state),
            fields.stream()
                .map(field -> field.accumulateNames(state))
                .collect(toList()),
            type
        );
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
            sourceRange,
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
    public SourceRange getSourceRange() {
        return sourceRange;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Value parsePrecedence(PrecedenceParser state) {
        return new Initializer(
            sourceRange,
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
        return new Initializer(sourceRange, value, fields, type);
    }

    private Value withFields(List<InitializerField> fields) {
        return new Initializer(sourceRange, value, fields, type);
    }

    private Initializer withValue(Value value) {
        return new Initializer(sourceRange, value, fields, type);
    }

    public static class Builder implements SyntaxBuilder<Initializer> {

        private Optional<SourceRange>            sourceRange;
        private Optional<Value>                  value;
        private Optional<List<InitializerField>> fields;
        private Optional<Type>                   type;

        private Builder() {
            sourceRange = Optional.empty();
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
                require(sourceRange, "Source range"),
                require(value, "Initializer value"),
                require(fields, "Initializer fields"),
                require(type, "Initializer type")
            );
        }

        @Override
        public Builder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
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
