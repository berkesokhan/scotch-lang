package scotch.symbol.descriptor;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import scotch.symbol.type.Type;

@AllArgsConstructor
@Getter
public class DataFieldDescriptor implements Comparable<DataFieldDescriptor> {

    public static DataFieldDescriptor field(int ordinal, String name, Type type) {
        return new DataFieldDescriptor(ordinal, name, type);
    }

    private final int    ordinal;
    private final String name;
    private final Type   type;

    @Override
    public int compareTo(DataFieldDescriptor o) {
        return ordinal - o.ordinal;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof DataFieldDescriptor) {
            DataFieldDescriptor other = (DataFieldDescriptor) o;
            return Objects.equals(ordinal, other.ordinal)
                && Objects.equals(name, other.name)
                && Objects.equals(type, other.type);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(ordinal, name, type);
    }

    @Override
    public String toString() {
        return name + " :: " + type;
    }

    public DataFieldDescriptor withType(Type type) {
        return new DataFieldDescriptor(ordinal, name, type);
    }
}
