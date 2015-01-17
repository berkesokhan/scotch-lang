package scotch.compiler.syntax.definition;

import java.util.HashSet;
import java.util.Set;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.text.SourceRange;

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

    public SourceRange getSourceRange() {
        return entry.getSourceRange();
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
