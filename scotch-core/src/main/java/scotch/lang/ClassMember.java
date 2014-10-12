package scotch.lang;

import static scotch.compiler.util.TextUtil.stringify;

import java.util.Objects;

public class ClassMember {

    public static ClassMember mandatoryMember(String name, Type type) {
        return member(name, true, type);
    }

    public static ClassMember member(String name, boolean mandatory, Type type) {
        return new ClassMember(name, mandatory, type);
    }

    public static ClassMember optionalMember(String name, Type type) {
        return member(name, false, type);
    }

    private final String  name;
    private final boolean mandatory;
    private final Type    type;

    private ClassMember(String name, boolean mandatory, Type type) {
        this.name = name;
        this.mandatory = mandatory;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof ClassMember) {
            ClassMember other = (ClassMember) o;
            return Objects.equals(name, other.name)
                && Objects.equals(mandatory, other.mandatory)
                && Objects.equals(type, other.type);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, mandatory, type);
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + name + ", " + (mandatory ? "required" : "optional") + ")";
    }
}
