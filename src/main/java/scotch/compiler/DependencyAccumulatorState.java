package scotch.compiler;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.definition.DefinitionEntry.entry;
import static scotch.compiler.syntax.reference.DefinitionReference.rootRef;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import scotch.compiler.error.SyntaxError;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.syntax.DependencyAccumulator;
import scotch.compiler.syntax.Scoped;
import scotch.compiler.syntax.definition.Definition;
import scotch.compiler.syntax.definition.DefinitionEntry;
import scotch.compiler.syntax.definition.DefinitionGraph;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.syntax.value.Identifier;
import scotch.compiler.syntax.value.PatternMatcher;
import scotch.compiler.syntax.value.Value;

public class DependencyAccumulatorState implements DependencyAccumulator {

    private final DefinitionGraph                           graph;
    private final Deque<Scope>                              scopes;
    private final Map<DefinitionReference, Scope>           functionScopes;
    private final Map<DefinitionReference, DefinitionEntry> entries;
    private final List<SyntaxError>                         errors;
    private final Deque<Symbol>                             symbols;

    public DependencyAccumulatorState(DefinitionGraph graph) {
        this.graph = graph;
        this.scopes = new ArrayDeque<>();
        this.functionScopes = new HashMap<>();
        this.entries = new HashMap<>();
        this.errors = new ArrayList<>();
        this.symbols = new ArrayDeque<>();
    }

    @Override
    public DefinitionGraph accumulateDependencies() {
        Definition root = getDefinition(rootRef()).orElseThrow(() -> new IllegalStateException("No root found!"));
        scoped(root, () -> root.accumulateDependencies(this));
        return graph
            .copyWith(entries.values())
            .appendErrors(errors)
            .build()
            .sort();
    }

    @Override
    public List<DefinitionReference> accumulateDependencies(List<DefinitionReference> references) {
        return references.stream()
            .map(this::getDefinition)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(definition -> definition.accumulateDependencies(this))
            .map(Definition::getReference)
            .collect(toList());
    }

    @Override
    public Identifier addDependency(Identifier identifier) {
        identifier.getSymbol().map(symbol -> {
            if (!symbols.contains(symbol)) {
                scope().addDependency(symbol);
            }
            return symbol;
        });
        return identifier;
    }

    @Override
    public Definition collect(Definition definition) {
        entries.put(definition.getReference(), entry(scope(), definition));
        return definition;
    }

    @Override
    public Definition collect(PatternMatcher pattern) {
        return collect(Value.scopeDef(pattern));
    }

    @Override
    public void enterScope(Definition definition) {
        enterScope(definition.getReference());
    }

    @Override
    public void error(SyntaxError error) {
        errors.add(error);
    }

    @Override
    public Optional<Definition> getDefinition(DefinitionReference reference) {
        return graph.getDefinition(reference);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Scoped> T keep(Scoped scoped) {
        return (T) scoped(scoped, () -> scoped);
    }

    @Override
    public void leaveScope() {
        scopes.pop();
    }

    @Override
    public void popSymbol() {
        symbols.pop();
    }

    @Override
    public void pushSymbol(Symbol symbol) {
        symbols.push(symbol);
    }

    @Override
    public Scope scope() {
        return scopes.peek();
    }

    @Override
    public <T extends Definition> T scoped(T definition, Supplier<? extends T> supplier) {
        enterScope(definition);
        try {
            T result = supplier.get();
            collect(result);
            return result;
        } finally {
            leaveScope();
        }
    }

    @Override
    public <T extends Scoped> T scoped(Scoped value, Supplier<? extends T> supplier) {
        enterScope(value.getReference());
        try {
            T result = supplier.get();
            collect(result.getDefinition());
            return result;
        } finally {
            leaveScope();
        }
    }

    private void enterScope(DefinitionReference reference) {
        scopes.push(getScope(reference));
    }

    private Scope getScope(DefinitionReference reference) {
        return graph.tryGetScope(reference).orElseGet(() -> functionScopes.get(reference));
    }
}
