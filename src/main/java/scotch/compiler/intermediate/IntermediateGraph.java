package scotch.compiler.intermediate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import scotch.compiler.syntax.reference.DefinitionReference;

public class IntermediateGraph {

    private final Map<DefinitionReference, IntermediateDefinition> definitions;

    public IntermediateGraph(List<IntermediateDefinition> definitions) {
        this.definitions = new HashMap<>();
        definitions.forEach(definition -> this.definitions.put(definition.getReference(), definition));
    }

    public Optional<IntermediateDefinition> getValue(DefinitionReference reference) {
        return Optional.ofNullable(definitions.get(reference));
    }
}
