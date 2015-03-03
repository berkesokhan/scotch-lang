package scotch.compiler.symbol.descriptor;

import static java.util.Collections.sort;
import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.google.common.collect.ImmutableList;
import scotch.compiler.symbol.Symbol;

public class DataConstructorDescriptor implements Comparable<DataConstructorDescriptor> {

    public static Builder builder(int ordinal, Symbol dataType, Symbol symbol) {
        return new Builder(ordinal, dataType, symbol);
    }

    private final int                       ordinal;
    private final Symbol                    dataType;
    private final Symbol                    symbol;
    private final List<DataFieldDescriptor> fields;

    private DataConstructorDescriptor(int ordinal, Symbol dataType, Symbol symbol, List<DataFieldDescriptor> fields) {
        List<DataFieldDescriptor> sortedFields = new ArrayList<>(fields);
        sort(sortedFields);
        this.ordinal = ordinal;
        this.dataType = dataType;
        this.symbol = symbol;
        this.fields = ImmutableList.copyOf(sortedFields);
    }

    @Override
    public int compareTo(DataConstructorDescriptor o) {
        return ordinal - o.ordinal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof DataConstructorDescriptor) {
            DataConstructorDescriptor other = (DataConstructorDescriptor) o;
            return Objects.equals(ordinal, other.ordinal)
                && Objects.equals(dataType, other.dataType)
                && Objects.equals(symbol, other.symbol)
                && Objects.equals(fields, other.fields);
        } else {
            return false;
        }
    }

    public Symbol getDataType() {
        return dataType;
    }

    public List<DataFieldDescriptor> getFields() {
        return fields;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ordinal, dataType, symbol, fields);
    }

    @Override
    public String toString() {
        return symbol.getSimpleName()
            + (fields.isEmpty() ? "" : " { " + fields.stream().map(Object::toString).collect(joining(", ")) + " }");
    }

    public static final class Builder {

        private final int                       ordinal;
        private final Symbol                    dataType;
        private final Symbol                    symbol;
        private       List<DataFieldDescriptor> fields;

        private Builder(int ordinal, Symbol dataType, Symbol symbol) {
            this.ordinal = ordinal;
            this.dataType = dataType;
            this.symbol = symbol;
            this.fields = new ArrayList<>();
        }

        public Builder addField(DataFieldDescriptor field) {
            this.fields.add(field);
            return this;
        }

        public DataConstructorDescriptor build() {
            sort(fields);
            return new DataConstructorDescriptor(
                ordinal,
                dataType,
                symbol,
                fields
            );
        }

        public Builder withFields(List<DataFieldDescriptor> fields) {
            fields.forEach(this::addField);
            return this;
        }
    }
}
