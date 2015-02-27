package scotch.compiler.steps;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.definition.DefinitionEntry.entry;
import static scotch.compiler.syntax.reference.DefinitionReference.rootRef;
import static scotch.compiler.syntax.value.Values.scopeDef;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import scotch.compiler.error.SyntaxError;
import scotch.compiler.symbol.descriptor.DataConstructorDescriptor;
import scotch.compiler.symbol.descriptor.DataTypeDescriptor;
import scotch.compiler.symbol.Operator;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.syntax.Scoped;
import scotch.compiler.syntax.definition.Definition;
import scotch.compiler.syntax.definition.DefinitionEntry;
import scotch.compiler.syntax.definition.DefinitionGraph;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.syntax.value.PatternMatcher;

public class NameAccumulator {

    private final DefinitionGraph                           graph;
    private final Deque<Scope>                              scopes;
    private final Map<DefinitionReference, Scope>           functionScopes;
    private final Map<DefinitionReference, DefinitionEntry> entries;
    private final List<SyntaxError>                         errors;

    public NameAccumulator(DefinitionGraph graph) {
        this.graph = graph;
        this.scopes = new ArrayDeque<>();
        this.functionScopes = new HashMap<>();
        this.entries = new HashMap<>();
        this.errors = new ArrayList<>();
    }

    public DefinitionGraph accumulateNames() {
        Definition root = getDefinition(rootRef()).orElseThrow(() -> new IllegalStateException("No root found!"));
        scoped(root, () -> root.accumulateNames(this));
        return graph
            .copyWith(entries.values())
            .appendErrors(errors)
            .build();
    }

    public List<DefinitionReference> accumulateNames(List<DefinitionReference> references) {
        return references.stream()
            .map(this::getDefinition)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(definition -> definition.accumulateNames(this))
            .map(Definition::getReference)
            .collect(toList());
    }

    public Definition collect(Definition definition) {
        entries.put(definition.getReference(), entry(scope(), definition));
        return definition;
    }

    public Definition collect(PatternMatcher pattern) {
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
    public <T extends Scoped> T keep(Scoped scoped) {
        return (T) scoped(scoped, () -> scoped);
    }

    public void leaveScope() {
        scopes.pop();
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

    public void defineDataConstructor(Symbol symbol, DataConstructorDescriptor descriptor) {
        scope().defineDataConstructor(symbol, descriptor);
    }

    public void defineDataType(Symbol symbol, DataTypeDescriptor descriptor) {
        scope().defineDataType(symbol, descriptor);
    }

    public void defineOperator(Symbol symbol, Operator operator) {
        scope().defineOperator(symbol, operator);
    }

    public void defineSignature(Symbol symbol, Type type) {
        scope().defineSignature(symbol, type);
    }

    public void defineValue(Symbol symbol, Type type) {
        scope().defineValue(symbol, type);
    }

    public boolean isOperator(Symbol symbol) {
        return scope().isOperator(symbol);
    }

    public void specialize(Type type) {
        scope().specialize(type);
    }
}
