package scotch.compiler.syntax.reference;

import java.util.List;
import com.google.common.collect.ImmutableList;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import scotch.symbol.descriptor.TypeParameterDescriptor;

@EqualsAndHashCode(callSuper = false)
@ToString
public class InstanceReference extends DefinitionReference {

    @Getter private final ClassReference                classReference;
    @Getter private final ModuleReference               moduleReference;
    @Getter private final List<TypeParameterDescriptor> parameters;

    InstanceReference(ClassReference classReference, ModuleReference moduleReference, List<TypeParameterDescriptor> parameters) {
        this.classReference = classReference;
        this.moduleReference = moduleReference;
        this.parameters = ImmutableList.copyOf(parameters);
    }
}
