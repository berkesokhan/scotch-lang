package scotch.compiler.syntax.definition;

import java.util.Set;
import scotch.symbol.Symbol;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.text.SourceRange;

public class DefinitionEntry {

    public static DefinitionEntry entry(Scope scope, Definition definition) {
        return new DefinitionEntry(scope, definition);
    }

    private final Scope scope;
    private final Definition definition;

    public DefinitionEntry(Scope scope, Definition definition) {
        this.scope = scope;
        this.definition = definition;
    }

    public Definition getDefinition() {
        return definition;
    }

    public Set<Symbol> getDependencies() {
        return scope.getDependencies();
    }

    public DefinitionReference getReference() {
        return definition.getReference();
    }

    public Scope getScope() {
        return scope;
    }

    public SourceRange getSourceRange() {
        return definition.getSourceRange();
    }

    public Symbol getSymbol() {
        return definition.asSymbol().orElseThrow(() -> new IllegalStateException("Definition " + definition.getReference() + " has no symbol"));
    }
}
