package scotch.compiler.syntax.definition;

import static java.util.stream.Collectors.joining;
import static scotch.compiler.text.SourceLocation.NULL_SOURCE;
import static scotch.compiler.text.TextUtil.repeat;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import com.google.common.collect.ImmutableSet;
import scotch.symbol.Symbol;
import scotch.compiler.text.SourceLocation;

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

    public String report(String indent, int indentLevel) {
        return nodes.stream()
            .map(node -> node.report(indent, indentLevel))
            .collect(joining("\n\n"));
    }

    public static final class Builder {

        private final Set<Node> nodes;

        private Builder() {
            nodes = new HashSet<>();
        }

        public Builder addNode(DefinitionNode node) {
            return addNode(node.getSymbol(), node.getSourceLocation(), node.getDependencies());
        }

        public Builder addNode(Symbol symbol, Collection<Symbol> dependencies) {
            return addNode(symbol, NULL_SOURCE, dependencies);
        }

        public Builder addNode(Symbol symbol, SourceLocation sourceLocation, Collection<Symbol> dependencies) {
            nodes.add(new Node(symbol, sourceLocation, dependencies));
            return this;
        }

        public DependencyCycle build() {
            return new DependencyCycle(nodes);
        }
    }

    public static final class Node {

        private final Symbol         symbol;
        private final SourceLocation sourceLocation;
        private final Set<Symbol>    dependencies;

        public Node(Symbol symbol, SourceLocation sourceLocation, Collection<Symbol> dependencies) {
            this.symbol = symbol;
            this.sourceLocation = sourceLocation;
            this.dependencies = ImmutableSet.copyOf(dependencies);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof Node) {
                Node other = (Node) o;
                return Objects.equals(symbol, other.symbol)
                    && Objects.equals(sourceLocation, other.sourceLocation)
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
            return "Method " + symbol.quote() + " depends on ["
                + dependencies.stream().map(Symbol::quote).collect(joining(", "))
                + "]" + " " + sourceLocation.prettyPrint();
        }

        public String report(String indent, int indentLevel) {
            return sourceLocation.report(indent, indentLevel) + "\n"
                + repeat(indent, indentLevel + 1) + "Can't analyze types! Dependency cycle detected:\n"
                + repeat(indent, indentLevel + 2) + "- " + symbol.quote() + "\n"
                + dependencies.stream()
                .map(symbol -> repeat(indent, indentLevel + 2) + "- " + symbol.quote())
                .collect(joining("\n"));
        }
    }
}
