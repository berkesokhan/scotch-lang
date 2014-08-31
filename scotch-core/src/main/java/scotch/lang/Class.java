package scotch.lang;

import static java.util.stream.Collectors.*;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import com.google.common.collect.ImmutableList;
import scotch.lang.Type.VariableType;

@SuppressWarnings("unused")
public class Class {

    public static Class cls(String name, List<VariableType> arguments, List<Function<Class, ClassMember>> members) {
        return new Class(name, arguments, members);
    }

    public static Function<Class, ClassMember> member(String name, Type type) {
        return owner -> new ClassMember(owner, name, type);
    }

    private final String name;
    private final List<VariableType> arguments;
    private final List<ClassMember> members;

    private Class(String name, List<VariableType> arguments, List<Function<Class, ClassMember>> members) {
        this.name = name;
        this.arguments = ImmutableList.copyOf(arguments);
        this.members = ImmutableList.copyOf(members.stream().map(fn -> fn.apply(this)).collect(toList()));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof Class) {
            Class other = (Class) o;
            return shallowEquals(other) && Objects.equals(members, other.members);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(shallowHashCode(), members);
    }

    @Override
    public String toString() {
        return "Class(" + name + ")";
    }

    private boolean shallowEquals(Class other) {
        return Objects.equals(name, other.name)
            && Objects.equals(arguments, other.arguments);
    }

    private int shallowHashCode() {
        return Objects.hash(name, arguments);
    }

    public static class ClassMember {

        private final Class owner;
        private final String name;
        private final Type type;

        private ClassMember(Class owner, String name, Type type) {
            this.owner = owner;
            this.name = name;
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof ClassMember) {
                ClassMember other = (ClassMember) o;
                return owner.shallowEquals(other.owner)
                    && Objects.equals(name, other.name)
                    && Objects.equals(type, other.type);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(owner.shallowHashCode(), name, type);
        }

        @Override
        public String toString() {
            return "(" + owner.name + ").(" + name + ")::" + type;
        }
    }
}
