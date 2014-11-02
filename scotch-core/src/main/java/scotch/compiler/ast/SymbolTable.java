package scotch.compiler.ast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import scotch.compiler.ast.Definition.DefinitionVisitor;
import scotch.compiler.ast.Definition.ValueDefinition;
import scotch.compiler.ast.DefinitionEntry.DefinitionEntryVisitor;
import scotch.compiler.ast.DefinitionEntry.ScopedEntry;
import scotch.compiler.ast.DefinitionEntry.UnscopedEntry;

public class SymbolTable {

    private final Map<DefinitionReference, DefinitionEntry> definitions;
    private final int                                       sequence;

    public SymbolTable(int sequence, List<DefinitionEntry> entries) {
        this.sequence = sequence;
        this.definitions = new HashMap<>();
        entries.forEach(entry -> this.definitions.put(entry.getReference(), entry));
    }

    public SymbolTable copyWith(List<DefinitionEntry> entries) {
        return copyWith(sequence, entries);
    }

    public SymbolTable copyWith(int sequence, List<DefinitionEntry> entries) {
        return new SymbolTable(sequence, entries);
    }

    public Definition getDefinition(DefinitionReference reference) {
        return definitions.get(reference).accept(new DefinitionEntryVisitor<Definition>() {
            @Override
            public Definition visit(ScopedEntry entry) {
                return entry.getDefinition();
            }

            @Override
            public Definition visit(UnscopedEntry entry) {
                return entry.getDefinition();
            }
        });
    }

    public Scope getScope(DefinitionReference reference) {
        return definitions.get(reference).getScope();
    }

    public int getSequence() {
        return sequence;
    }

    public Type getType(DefinitionReference reference) {
        return getDefinition(reference).accept(new DefinitionVisitor<Type>() {
            @Override
            public Type visit(ValueDefinition definition) {
                return definition.getType();
            }

            @Override
            public Type visitOtherwise(Definition definition) {
                throw new IllegalArgumentException("Can't get type of " + definition.getClass().getSimpleName());
            }
        });
    }
}
