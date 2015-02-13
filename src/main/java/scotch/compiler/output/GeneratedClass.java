package scotch.compiler.output;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
public class GeneratedClass implements Comparable<GeneratedClass> {

    @NonNull
    private final ClassType type;
    @NonNull @Getter
    private final String    className;
    @NonNull @Getter
    private final byte[]    bytes;

    @Override
    public int compareTo(GeneratedClass o) {
        int typeCompare = type.compareTo(o.type);
        if (typeCompare == 0) {
            return className.compareTo(o.className);
        } else {
            return typeCompare;
        }
    }

    public enum ClassType {
        DATA_TYPE,
        DATA_CONSTRUCTOR,
        MODULE,
    }
}
