package scotch.compiler.syntax.reference;

import static scotch.util.StringUtil.stringify;

import java.util.Objects;

public class RootReference extends DefinitionReference {

    RootReference() {
        // intentionally empty
    }

    @Override
    public boolean equals(Object o) {
        return o == this;
    }

    @Override
    public int hashCode() {
        return Objects.hash();
    }

    @Override
    public String toString() {
        return stringify(this);
    }
}
