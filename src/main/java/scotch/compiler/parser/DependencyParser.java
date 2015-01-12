package scotch.compiler.parser;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.DefinitionReference.rootRef;
import static scotch.compiler.syntax.SyntaxError.cyclicDependency;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Symbol.QualifiedSymbol;
import scotch.compiler.symbol.Symbol.SymbolVisitor;
import scotch.compiler.symbol.Symbol.UnqualifiedSymbol;
import scotch.compiler.syntax.Definition.DefinitionVisitor;
import scotch.compiler.syntax.Definition.ModuleDefinition;
import scotch.compiler.syntax.Definition.OperatorDefinition;
import scotch.compiler.syntax.Definition.RootDefinition;
import scotch.compiler.syntax.Definition.ValueDefinition;
import scotch.compiler.syntax.Definition.ValueSignature;
import scotch.compiler.syntax.DefinitionEntry;
import scotch.compiler.syntax.DefinitionGraph;
import scotch.compiler.syntax.DefinitionReference;
import scotch.compiler.syntax.DefinitionReference.DefinitionReferenceVisitor;
import scotch.compiler.syntax.DefinitionReference.ValueReference;
import scotch.compiler.syntax.DependencyCycle;
import scotch.compiler.syntax.PatternMatch;
import scotch.compiler.syntax.PatternMatch.EqualMatch;
import scotch.compiler.syntax.PatternMatch.PatternMatchVisitor;
import scotch.compiler.syntax.Scope;
import scotch.compiler.syntax.SyntaxError;
import scotch.compiler.syntax.Value.Apply;
import scotch.compiler.syntax.Value.BoolLiteral;
import scotch.compiler.syntax.Value.CharLiteral;
import scotch.compiler.syntax.Value.DoubleLiteral;
import scotch.compiler.syntax.Value.FunctionValue;
import scotch.compiler.syntax.Value.Identifier;
import scotch.compiler.syntax.Value.IntLiteral;
import scotch.compiler.syntax.Value.PatternMatchers;
import scotch.compiler.syntax.Value.StringLiteral;
import scotch.compiler.syntax.Value.ValueVisitor;
import scotch.compiler.text.SourceRange;

public class DependencyParser implements
    PatternMatchVisitor<Void>,
    ValueVisitor<Void>,
    DefinitionVisitor<Void>,
    DefinitionReferenceVisitor<Void> {

    private final DefinitionGraph   graph;
    private final Deque<Scope>      scopes;
    private final Deque<Symbol>     symbols;
    private final List<SyntaxError> errors;

    public DependencyParser(DefinitionGraph graph) {
        this.graph = graph;
        this.scopes = new ArrayDeque<>();
        this.symbols = new ArrayDeque<>();
        this.errors = new ArrayList<>();
    }

    public DefinitionGraph parse() {
        rootRef().accept(this);
        return graph
            .copyWith(sort())
            .appendErrors(errors)
            .build();
    }

    @Override
    public Void visit(Apply apply) {
        apply.getFunction().accept(this);
        apply.getArgument().accept(this);
        return null;
    }

    @Override
    public Void visit(BoolLiteral literal) {
        return null;
    }

    @Override
    public Void visit(CharLiteral literal) {
        return null;
    }

    @Override
    public Void visit(DoubleLiteral literal) {
        return null;
    }

    @Override
    public Void visit(EqualMatch match) {
        match.getValue().accept(this);
        return null;
    }

    @Override
    public Void visit(FunctionValue function) {
        function.getBody().accept(this);
        return null;
    }

    @Override
    public Void visit(Identifier identifier) {
        return identifier.getSymbol().accept(new SymbolVisitor<Void>() {
            @Override
            public Void visit(QualifiedSymbol symbol) {
                if (!symbols.peek().equals(symbol)) {
                    currentScope().addDependency(symbol);
                }
                return null;
            }

            @Override
            public Void visit(UnqualifiedSymbol symbol) {
                return null;
            }
        });
    }

    @Override
    public Void visit(IntLiteral literal) {
        return null;
    }

    @Override
    public Void visit(ModuleDefinition definition) {
        definition.getDefinitions().forEach(reference -> reference.accept(this));
        return null;
    }

    @Override
    public Void visit(OperatorDefinition definition) {
        return null;
    }

    @Override
    public Void visit(PatternMatchers matchers) {
        matchers.getMatchers().forEach(matcher -> {
            matcher.getMatches().forEach(match -> match.accept(this));
            matcher.getBody().accept(this);
        });
        return null;
    }

    @Override
    public Void visit(RootDefinition definition) {
        definition.getDefinitions().forEach(reference -> reference.accept(this));
        return null;
    }

    @Override
    public Void visit(StringLiteral literal) {
        return null;
    }

    @Override
    public Void visit(ValueDefinition definition) {
        symbols.push(definition.getSymbol());
        try {
            definition.getBody().accept(this);
            return null;
        } finally {
            symbols.pop();
        }
    }

    @Override
    public Void visit(ValueSignature signature) {
        return null;
    }

    @Override
    public Void visitOtherwise(PatternMatch match) {
        return null; // TODO
    }

    @Override
    public Void visitOtherwise(DefinitionReference reference) {
        return scoped(reference, () -> {
            graph.getDefinition(reference).map(definition -> definition.accept(this));
            return null;
        });
    }

    private Scope currentScope() {
        return scopes.peek();
    }

    private <T> T scoped(DefinitionReference reference, Supplier<T> supplier) {
        scopes.push(graph.getScope(reference));
        try {
            return supplier.get();
        } finally {
            scopes.pop();
        }
    }

    private List<DefinitionEntry> sort() {
        List<DefinitionEntry> entries = graph.stream()
            .filter(entry -> !(entry.getReference() instanceof ValueReference))
            .collect(toList());
        List<DefinitionNode> values = graph.stream()
            .filter(entry -> entry.getReference() instanceof ValueReference)
            .map(DefinitionNode::new)
            .collect(toList());
        entries.addAll(sort_(values));
        return entries;
    }

    private List<DefinitionEntry> sort_(List<DefinitionNode> input) {
        List<DefinitionNode> roots = new ArrayList<>();
        List<DefinitionNode> nodes = new ArrayList<>();
        List<DefinitionEntry> output = new ArrayList<>();
        input.forEach(node -> {
            if (node.hasDependencies()) {
                nodes.add(node);
            } else {
                roots.add(node);
            }
        });
        while (!roots.isEmpty()) {
            DefinitionNode root = roots.remove(0);
            output.add(root.getEntry());
            Iterator<DefinitionNode> iterator = nodes.iterator();
            while (iterator.hasNext()) {
                DefinitionNode node = iterator.next();
                if (node.isDependentOn(root)) {
                    node.removeDependency(root);
                    if (!node.hasDependencies()) {
                        roots.add(node);
                        iterator.remove();
                    }
                }
            }
        }
        if (nodes.isEmpty()) {
            return output;
        } else {
            errors.add(cyclicDependency(DependencyCycle.fromNodes(nodes)));
            output.addAll(nodes.stream().map(DefinitionNode::getEntry).collect(toList()));
            return output;
        }
    }

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
}
