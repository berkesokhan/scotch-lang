package scotch.compiler.intermediate;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import scotch.compiler.syntax.reference.DefinitionReference;

@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ToString
public class IntermediateDefinition {

    private final DefinitionReference reference;
    private final IntermediateValue   value;

    public DefinitionReference getReference() {
        return reference;
    }
}
