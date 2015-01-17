package scotch.compiler.syntax.definition;

import static java.util.stream.Collectors.joining;
import static scotch.compiler.text.SourceRange.NULL_SOURCE;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import com.google.common.collect.ImmutableSet;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.text.SourceRange;

public class DependencyCycle {

    private final Set<Node> nodes;

    public DependencyCycle(Set<Node> nodes) {
        this.nodes = ImmutableSet.copyOf(nodes);
    }

    public static DependencyCycle.Builder builder() {
        return new DependencyCycle.Builder();
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof DependencyCycle && Objects.equals(nodes, ((DependencyCycle) o).nodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodes);
    }

    public String prettyPrint() {
        return "Dependency cycle detected:\n" + nodes.stream().map(Node::prettyPrint).collect(joining("\n"));
    }

    public static final class Builder {

        private final Set<Node> nodes;

        private Builder() {
            nodes = new HashSet<>();
        }

        public Builder addNode(DefinitionNode node) {
            return addNode(node.getSymbol(), node.getSourceRange(), node.getDependencies());
        }

        public Builder addNode(Symbol symbol, Collection<Symbol> dependencies) {
            return addNode(symbol, NULL_SOURCE, dependencies);
        }

        public Builder addNode(Symbol symbol, SourceRange sourceRange, Collection<Symbol> dependencies) {
            nodes.add(new Node(symbol, sourceRange, dependencies));
            return this;
        }

        public DependencyCycle build() {
            return new DependencyCycle(nodes);
        }
    }

    public static final class Node {

        private final Symbol      symbol;
        private final SourceRange sourceRange;
        private final Set<Symbol> dependencies;

        public Node(Symbol symbol, SourceRange sourceRange, Collection<Symbol> dependencies) {
            this.symbol = symbol;
            this.sourceRange = sourceRange;
            this.dependencies = ImmutableSet.copyOf(dependencies);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof Node) {
                Node other = (Node) o;
                return Objects.equals(symbol, other.symbol)
                    && Objects.equals(sourceRange, other.sourceRange)
                    && Objects.equals(dependencies, other.dependencies);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol, dependencies);
        }

        public String prettyPrint() {
            return "Value " + symbol.quote() + " depends on ["
                + dependencies.stream().map(Symbol::quote).collect(joining(", "))
                + "] " + sourceRange.prettyPrint();
        }
    }
}
