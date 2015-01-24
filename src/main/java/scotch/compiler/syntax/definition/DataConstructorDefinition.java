package scotch.compiler.syntax.definition;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.compiler.syntax.value.Value.construct;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.syntax.value.Constant;
import scotch.compiler.syntax.value.FunctionValue;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.text.SourceRange;

public class DataConstructorDefinition {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceRange                      sourceRange;
    private final Symbol                           symbol;
    private final Map<String, DataFieldDefinition> fields;

    public DataConstructorDefinition(SourceRange sourceRange, Symbol symbol, List<DataFieldDefinition> fields) {
        this.sourceRange = sourceRange;
        this.symbol = symbol;
        this.fields = new LinkedHashMap<>();
        fields.forEach(field -> this.fields.put(field.getName(), field));
    }

    public ValueDefinition createValue(Scope scope) {
        Value body;
        if (fields.isEmpty()) {
            body = Constant.builder()
                .withSourceRange(sourceRange)
                .withSymbol(symbol)
                .withType(scope.reserveType())
                .build();
        } else {
            body = FunctionValue.builder()
                .withSourceRange(sourceRange)
                .withSymbol(scope.reserveSymbol())
                .withArguments(fields.values().stream()
                    .map(field -> field.toArgument(scope))
                    .collect(toList()))
                .withBody(construct(sourceRange, symbol, scope.reserveType(), fields.values().stream()
                    .map(field -> field.toValue(scope))
                    .collect(toList())))
                .build();
        }
        return ValueDefinition.builder()
            .withSourceRange(sourceRange)
            .withSymbol(symbol)
            .withType(scope.reserveType())
            .withBody(body)
            .build();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return false;
        } else if (o instanceof DataConstructorDefinition) {
            DataConstructorDefinition other = (DataConstructorDefinition) o;
            return Objects.equals(sourceRange, other.sourceRange)
                && Objects.equals(symbol, other.symbol)
                && Objects.equals(fields, other.fields);
        } else {
            return false;
        }
    }

    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, fields);
    }

    @Override
    public String toString() {
        return symbol.getSimpleName()
            + (fields.isEmpty() ? "" : " { " + fields.values().stream().map(Object::toString).collect(joining(", ")) + " }");
    }

    public static class Builder implements SyntaxBuilder<DataConstructorDefinition> {

        private Optional<SourceRange>               sourceRange;
        private Optional<Symbol>                    symbol;
        private Optional<Symbol>                    dataType;
        private List<DataFieldDefinition> fields;

        private Builder() {
            sourceRange = Optional.empty();
            symbol = Optional.empty();
            dataType = Optional.empty();
            fields = new ArrayList<>();
        }

        public Builder addField(DataFieldDefinition field) {
            fields.add(field);
            return this;
        }

        @Override
        public DataConstructorDefinition build() {
            return new DataConstructorDefinition(
                require(sourceRange, "Source range"),
                require(symbol, "Constructor symbol"),
                fields
            );
        }

        public Builder withDataType(Symbol dataType) {
            this.dataType = Optional.of(dataType);
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
