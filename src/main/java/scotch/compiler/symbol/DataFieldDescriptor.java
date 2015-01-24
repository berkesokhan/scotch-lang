package scotch.compiler.symbol;

import java.util.Objects;

public class DataFieldDescriptor {

    public static DataFieldDescriptor field(String name, Type type) {
        return new DataFieldDescriptor(name, type);
    }

    private final String name;
    private final Type   type;

    private DataFieldDescriptor(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof DataFieldDescriptor) {
            DataFieldDescriptor other = (DataFieldDescriptor) o;
            return Objects.equals(name, other.name)
                && Objects.equals(type, other.type);
        } else {
            return false;
        }
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public String toString() {
        return name + " " + type;
    }
}
