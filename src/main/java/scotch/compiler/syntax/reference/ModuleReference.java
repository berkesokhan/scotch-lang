package scotch.compiler.syntax.reference;

import static scotch.util.StringUtil.stringify;

import java.util.Objects;

public class ModuleReference extends DefinitionReference {

    private final String name;

    ModuleReference(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof ModuleReference && Objects.equals(name, ((ModuleReference) o).name);
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public boolean is(String otherName) {
        return Objects.equals(name, otherName);
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + name + ")";
    }
}
