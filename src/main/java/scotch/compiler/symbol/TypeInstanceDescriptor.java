package scotch.compiler.symbol;

import static java.util.stream.Collectors.joining;
import static scotch.util.StringUtil.stringify;

import java.util.List;
import java.util.Objects;
import me.qmx.jitescript.CodeBlock;

public class TypeInstanceDescriptor {

    public static TypeInstanceDescriptor typeInstance(String moduleName, Symbol typeClass, List<Type> arguments, MethodSignature instanceGetter) {
        return new TypeInstanceDescriptor(moduleName, typeClass, arguments, instanceGetter);
    }

    private final String          moduleName;
    private final Symbol          typeClass;
    private final List<Type>      parameters;
    private final MethodSignature instanceGetter;

    private TypeInstanceDescriptor(String moduleName, Symbol typeClass, List<Type> parameters, MethodSignature instanceGetter) {
        this.moduleName = moduleName;
        this.typeClass = typeClass;
        this.parameters = parameters;
        this.instanceGetter = instanceGetter;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof TypeInstanceDescriptor) {
            TypeInstanceDescriptor other = (TypeInstanceDescriptor) o;
            return Objects.equals(moduleName, other.moduleName)
                && Objects.equals(typeClass, other.typeClass)
                && Objects.equals(parameters, other.parameters)
                && Objects.equals(instanceGetter, other.instanceGetter);
        } else {
            return false;
        }
    }

    public String getModuleName() {
        return moduleName;
    }

    public List<Type> getParameters() {
        return parameters;
    }

    public Symbol getTypeClass() {
        return typeClass;
    }

    @Override
    public int hashCode() {
        return Objects.hash(moduleName, typeClass, parameters, instanceGetter);
    }

    public CodeBlock reference() {
        return instanceGetter.reference();
    }

    @Override
    public String toString() {
        return stringify(this) + "("
            + moduleName + ":" + typeClass.getCanonicalName()
            + ", ["
            + parameters.stream().map(Type::toString).collect(joining(", "))
            + "])";
    }
}
