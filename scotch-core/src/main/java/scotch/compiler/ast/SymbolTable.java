package scotch.compiler.ast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymbolTable {

    private final Map<DefinitionReference, DefinitionEntry> definitions;
    private final int                                       sequence;

    public SymbolTable(int sequence, List<DefinitionEntry> entries) {
        this.sequence = sequence;
        this.definitions = new HashMap<>();
        entries.forEach(entry -> this.definitions.put(entry.getReference(), entry));
    }

    public int getSequence() {
        return sequence;
    }

    public Definition getDefinition(DefinitionReference reference) {
        return definitions.get(reference).getDefinition();
    }
}
