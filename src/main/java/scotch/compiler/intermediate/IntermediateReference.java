package scotch.compiler.intermediate;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import scotch.compiler.syntax.reference.DefinitionReference;

@EqualsAndHashCode(callSuper = false)
@ToString
public class IntermediateReference extends IntermediateValue {

    private final DefinitionReference reference;

    public IntermediateReference(DefinitionReference reference) {
        this.reference = reference;
    }
}
