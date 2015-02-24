package scotch.compiler.symbol;

import lombok.EqualsAndHashCode;
import scotch.compiler.symbol.type.Type;

@EqualsAndHashCode(callSuper = false)
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

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return name + " " + type;
    }

    public DataFieldDescriptor withType(Type type) {
        return new DataFieldDescriptor(name, type);
    }
}
