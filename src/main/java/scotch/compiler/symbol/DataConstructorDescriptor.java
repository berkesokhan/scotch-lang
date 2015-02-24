package scotch.compiler.symbol;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
public class DataConstructorDescriptor {

    public static Builder builder(Symbol dataType, Symbol symbol) {
        return new Builder(dataType, symbol);
    }

    private final Symbol                           dataType;
    private final Symbol                           symbol;
    private final Map<String, DataFieldDescriptor> fields;

    private DataConstructorDescriptor(Symbol dataType, Symbol symbol, List<DataFieldDescriptor> fields) {
        this.dataType = dataType;
        this.symbol = symbol;
        this.fields = new LinkedHashMap<>();
        fields.forEach(field -> this.fields.put(field.getName(), field));
    }

    public Symbol getDataType() {
        return dataType;
    }

    public Map<String, DataFieldDescriptor> getFieldMap() {
        return fields;
    }

    public List<DataFieldDescriptor> getFields() {
        return new ArrayList<>(fields.values());
    }

    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public String toString() {
        return symbol.getSimpleName()
            + (fields.isEmpty() ? "" : " { " + fields.values().stream().map(Object::toString).collect(joining(", ")) + " }");
    }

    public static final class Builder {

        private final Symbol                    dataType;
        private final Symbol                    symbol;
        private       List<DataFieldDescriptor> fields;

        private Builder(Symbol dataType, Symbol symbol) {
            this.dataType = dataType;
            this.symbol = symbol;
            this.fields = new ArrayList<>();
        }

        public DataConstructorDescriptor build() {
            return new DataConstructorDescriptor(
                dataType,
                symbol,
                fields
            );
        }

        public Builder addField(DataFieldDescriptor field) {
            this.fields.add(field);
            return this;
        }

        public Builder withFields(List<DataFieldDescriptor> fields) {
            fields.forEach(this::addField);
            return this;
        }
    }
}
