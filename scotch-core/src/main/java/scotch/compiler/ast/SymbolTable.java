package scotch.compiler.ast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import scotch.compiler.ast.DefinitionEntry.DefinitionEntryVisitor;
import scotch.compiler.ast.DefinitionEntry.ScopedEntry;

public class SymbolTable {

    private final Map<DefinitionReference, DefinitionEntry> definitions;

    public SymbolTable(List<DefinitionEntry> entries) {
        this.definitions = new HashMap<>();
        entries.forEach(entry -> this.definitions.put(entry.getReference(), entry));
    }

    public Definition getDefinition(DefinitionReference reference) {
        return definitions.get(reference).getDefinition();
    }

    public Scope getScope(DefinitionReference reference) {
        return definitions.get(reference).accept(new DefinitionEntryVisitor<Scope>() {
            @Override
            public Scope visit(ScopedEntry entry) {
                return entry.getScope();
            }
        });
    }

    public void setDefinition(DefinitionReference reference, Definition definition) {
        definitions.get(reference).setDefinition(definition);
    }
}
