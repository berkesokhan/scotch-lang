package scotch.compiler.syntax.definition;

import java.util.HashSet;
import java.util.Set;
import scotch.symbol.Symbol;
import scotch.compiler.text.SourceLocation;

public final class DefinitionNode {

    private final DefinitionEntry entry;
    private final Set<Symbol>     dependencies;

    public DefinitionNode(DefinitionEntry entry) {
        this.entry = entry;
        this.dependencies = new HashSet<>(entry.getDependencies());
    }

    public Set<Symbol> getDependencies() {
        return dependencies;
    }

    public DefinitionEntry getEntry() {
        return entry;
    }

    public SourceLocation getSourceLocation() {
        return entry.getSourceLocation();
    }

    public Symbol getSymbol() {
        return entry.getSymbol();
    }

    public boolean hasDependencies() {
        return !dependencies.isEmpty();
    }

    public boolean isDependentOn(DefinitionNode node) {
        return dependencies.contains(node.getSymbol());
    }

    public void removeDependency(DefinitionNode node) {
        dependencies.remove(node.getSymbol());
    }
}
