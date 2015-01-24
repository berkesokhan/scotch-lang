package scotch.compiler.syntax.definition;

import static java.util.stream.Collectors.joining;
import static scotch.compiler.syntax.builder.BuilderUtil.require;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.syntax.builder.SyntaxBuilder;
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
