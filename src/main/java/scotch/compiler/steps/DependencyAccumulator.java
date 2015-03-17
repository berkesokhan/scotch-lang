package scotch.compiler.steps;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.definition.DefinitionEntry.entry;
import static scotch.compiler.syntax.reference.DefinitionReference.rootRef;
import static scotch.compiler.syntax.definition.Definitions.scopeDef;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import scotch.compiler.error.SyntaxError;
import scotch.symbol.Symbol;
import scotch.symbol.type.Type;
import scotch.compiler.syntax.Scoped;
import scotch.compiler.syntax.definition.Definition;
import scotch.compiler.syntax.definition.DefinitionEntry;
import scotch.compiler.syntax.definition.DefinitionGraph;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.syntax.value.Identifier;
import scotch.compiler.syntax.pattern.PatternCase;

public class DependencyAccumulator {

    private final DefinitionGraph                           graph;
    private final Deque<Scope>                              scopes;
    private final Map<DefinitionReference, Scope>           functionScopes;
    private final Map<DefinitionReference, DefinitionEntry> entries;
    private final List<SyntaxError>                         errors;
    private final Deque<Symbol>                             symbols;

    public DependencyAccumulator(DefinitionGraph graph) {
        this.graph = graph;
        this.scopes = new ArrayDeque<>();
        this.functionScopes = new HashMap<>();
        this.entries = new HashMap<>();
        this.errors = new ArrayList<>();
        this.symbols = new ArrayDeque<>();
    }

    public DefinitionGraph accumulateDependencies() {
        Definition root = getDefinition(rootRef()).orElseThrow(() -> new IllegalStateException("No root found!"));
        scoped(root, () -> root.accumulateDependencies(this));
        return graph
            .copyWith(entries.values())
            .appendErrors(errors)
            .build()
            .sort();
    }

    public List<DefinitionReference> accumulateDependencies(List<DefinitionReference> references) {
        return references.stream()
            .map(this::getDefinition)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(definition -> definition.accumulateDependencies(this))
            .map(Definition::getReference)
            .collect(toList());
    }

    public Identifier addDependency(Identifier identifier) {
        return identifier.withSymbol(addDependency(identifier.getSymbol()));
    }

    public Symbol addDependency(Symbol symbol) {
        return symbol.map(qualifiedSymbol -> {
            if (!symbols.contains(qualifiedSymbol)) {
                scope().addDependency(qualifiedSymbol);
            }
            return qualifiedSymbol;
        });
    }

    public Definition collect(Definition definition) {
        entries.put(definition.getReference(), entry(scope(), definition));
        return definition;
    }

    public Definition collect(PatternCase pattern) {
        return collect(scopeDef(pattern));
    }

    public void enterScope(Definition definition) {
        enterScope(definition.getReference());
    }

    public void error(SyntaxError error) {
        errors.add(error);
    }

    public Optional<Definition> getDefinition(DefinitionReference reference) {
        return graph.getDefinition(reference);
    }

    @SuppressWarnings("unchecked")
    public <T extends Scoped> T keep(Scoped scopedThing) {
        return (T) scoped(scopedThing, () -> scopedThing);
    }

    public void leaveScope() {
        scopes.pop();
    }

    public void popSymbol() {
        symbols.pop();
    }

    public void pushSymbol(Symbol symbol) {
        symbols.push(symbol);
    }

    public Scope scope() {
        return scopes.peek();
    }

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

    public void defineValue(Symbol symbol, Type type) {
        scope().defineValue(symbol, type);
    }

    public boolean isOperator(Symbol symbol) {
        return scope().isOperator(symbol);
    }
}
