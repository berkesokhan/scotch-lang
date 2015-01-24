package scotch.compiler.syntax.reference;

import static scotch.util.StringUtil.stringify;

import java.util.List;
import java.util.Objects;
import com.google.common.collect.ImmutableList;
import scotch.compiler.symbol.Type;

public class InstanceReference extends DefinitionReference {

    private final ClassReference  classReference;
    private final ModuleReference moduleReference;
    private final List<Type>      types;

    InstanceReference(ClassReference classReference, ModuleReference moduleReference, List<Type> types) {
        this.classReference = classReference;
        this.moduleReference = moduleReference;
        this.types = ImmutableList.copyOf(types);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof DefinitionReference) {
            InstanceReference other = (InstanceReference) o;
            return Objects.equals(classReference, other.classReference)
                && Objects.equals(moduleReference, other.moduleReference)
                && Objects.equals(types, other.types);
        } else {
            return false;
        }
    }

    public ClassReference getClassReference() {
        return classReference;
    }

    public ModuleReference getModuleReference() {
        return moduleReference;
    }

    public List<Type> getTypes() {
        return types;
    }

    @Override
    public int hashCode() {
        return Objects.hash(classReference, moduleReference, types);
    }

    @Override
    public String toString() {
        return stringify(this) + "(classReference=" + classReference + ", moduleReference=" + moduleReference + ", types=" + types + ")";
    }
}
