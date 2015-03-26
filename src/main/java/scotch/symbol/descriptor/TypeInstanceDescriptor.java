package scotch.symbol.descriptor;

import static java.util.stream.Collectors.toList;

import java.util.List;
import com.google.common.collect.ImmutableList;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import me.qmx.jitescript.CodeBlock;
import scotch.symbol.MethodSignature;
import scotch.symbol.Symbol;
import scotch.symbol.TypeParameter;
import scotch.symbol.type.Type;

@EqualsAndHashCode
@ToString
public class TypeInstanceDescriptor {

    public static TypeInstanceDescriptor typeInstance(String moduleName, Symbol typeClass, List arguments, MethodSignature instanceGetter) {
        return new TypeInstanceDescriptor(moduleName, typeClass, parameterize(arguments), instanceGetter);
    }

    @SuppressWarnings("unchecked")
    private static List<TypeParameterDescriptor> parameterize(List arguments) {
        if (arguments.size() == 0) {
            return ImmutableList.of();
        } else if (arguments.get(0) instanceof TypeParameter) {
            return arguments;
        } else if (arguments.get(0) instanceof Type) {
            return ((List<Type>) arguments).stream()
                .map(TypeParameterDescriptor::typeParam)
                .collect(toList());
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Getter private final String                        moduleName;
    @Getter private final Symbol                        typeClass;
    @Getter private final List<TypeParameterDescriptor> parameters;
    private final         MethodSignature               instanceGetter;

    private TypeInstanceDescriptor(String moduleName, Symbol typeClass, List<TypeParameterDescriptor> parameters, MethodSignature instanceGetter) {
        this.moduleName = moduleName;
        this.typeClass = typeClass;
        this.parameters = ImmutableList.copyOf(parameters);
        this.instanceGetter = instanceGetter;
    }

    public CodeBlock reference() {
        return instanceGetter.reference();
    }
}
