package scotch.compiler.symbol;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class DataConstructorDescriptor {

    public static Builder builder(Symbol symbol) {
        return new Builder(symbol);
    }

    private final Symbol                           symbol;
    private final Symbol                           dataType;
    private final Map<String, DataFieldDescriptor> fields;

    public DataConstructorDescriptor(Symbol symbol, Symbol dataType, List<DataFieldDescriptor> fields) {
        this.symbol = symbol;
        this.dataType = dataType;
        this.fields = new LinkedHashMap<>();
        fields.forEach(field -> this.fields.put(field.getName(), field));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof DataConstructorDescriptor) {
            DataConstructorDescriptor other = (DataConstructorDescriptor) o;
            return Objects.equals(symbol, other.symbol)
                && Objects.equals(dataType, other.dataType)
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
        return Objects.hash(symbol, dataType, fields);
    }

    @Override
    public String toString() {
        return symbol.getSimpleName()
            + (fields.isEmpty() ? "" : " { " + fields.values().stream().map(Object::toString).collect(joining(", ")) + " }");
    }

    public static final class Builder {

        private Symbol                    symbol;
        private Optional<Symbol>          dataType;
        private List<DataFieldDescriptor> fields;

        private Builder(Symbol symbol) {
            this.symbol = symbol;
            this.dataType = Optional.empty();
            this.fields = new ArrayList<>();
        }

        public DataConstructorDescriptor build() {
            return new DataConstructorDescriptor(
                symbol,
                dataType.orElseThrow(() -> new IllegalStateException("No data type given for " + symbol.quote())),
                fields
            );
        }

        public Builder withDataType(Symbol dataType) {
            this.dataType = Optional.of(dataType);
            return this;
        }

        public Builder addField(DataFieldDescriptor field) {
            this.fields.add(field);
            return this;
        }
    }
}
