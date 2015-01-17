package scotch.compiler.syntax.definition;

import java.util.Set;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.reference.DefinitionReference.DefinitionReferenceVisitor;
import scotch.compiler.syntax.reference.ValueReference;
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
        return definition.getReference().accept(new DefinitionReferenceVisitor<Symbol>() {
            @Override
            public Symbol visit(ValueReference reference) {
                return reference.getSymbol();
            }
        });
    }

    public DefinitionEntry withScope(Scope scope) {
        return new DefinitionEntry(scope, definition);
    }
}
