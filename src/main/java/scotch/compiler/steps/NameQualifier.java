package scotch.compiler.steps;

import static java.util.Arrays.asList;
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
import com.google.common.collect.ImmutableList;
import scotch.compiler.error.SyntaxError;
import scotch.compiler.symbol.Operator;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Symbol.QualifiedSymbol;
import scotch.compiler.symbol.Symbol.SymbolVisitor;
import scotch.compiler.symbol.Symbol.UnqualifiedSymbol;
import scotch.compiler.symbol.SymbolNotFoundError;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.syntax.Scoped;
import scotch.compiler.syntax.definition.Definition;
import scotch.compiler.syntax.definition.DefinitionEntry;
import scotch.compiler.syntax.definition.DefinitionGraph;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.syntax.value.FunctionValue;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.text.SourceRange;

public class NameQualifier {

    private final DefinitionGraph                           graph;
    private final Deque<Scope>                              scopes;
    private final Map<DefinitionReference, Scope>           functionScopes;
    private final Map<DefinitionReference, DefinitionEntry> entries;
    private final Deque<List<String>>                       memberNames;
    private final List<SyntaxError>                         errors;

    public NameQualifier(DefinitionGraph graph) {
        this.graph = graph;
        this.scopes = new ArrayDeque<>();
        this.functionScopes = new HashMap<>();
        this.entries = new HashMap<>();
        this.memberNames = new ArrayDeque<>(asList(ImmutableList.of()));
        this.errors = new ArrayList<>();
    }

    public Definition collect(Definition definition) {
        entries.put(definition.getReference(), entry(scope(), definition));
        return definition;
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

    public <T> T named(Symbol symbol, Supplier<T> supplier) {
        memberNames.push(symbol.getMemberNames());
        T result = supplier.get();
        memberNames.pop();
        return result;
    }

    public DefinitionGraph qualifyNames() {
        Definition root = getDefinition(rootRef()).orElseThrow(() -> new IllegalStateException("No root found!"));
        scoped(root, () -> root.qualifyNames(this));
        return graph
            .copyWith(entries.values())
            .appendErrors(errors)
            .build();
    }

    public void symbolNotFound(Symbol symbol, SourceRange sourceRange) {
        errors.add(SymbolNotFoundError.symbolNotFound(symbol, sourceRange));
    }

    public Optional<Symbol> qualify(Symbol symbol) {
        return symbol.accept(new SymbolVisitor<Optional<Symbol>>() {
            @Override
            public Optional<Symbol> visit(QualifiedSymbol symbol) {
                return Optional.of(symbol);
            }

            @Override
            public Optional<Symbol> visit(UnqualifiedSymbol symbol) {
                List<Symbol> result = memberNames.stream()
                    .map(symbol::nest)
                    .map(scope()::qualify)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(toList());
                if (result.isEmpty()) {
                    return Optional.empty();
                } else {
                    return Optional.of(result.get(0));
                }
            }
        });
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
        if (value instanceof FunctionValue) {
            memberNames.push(((FunctionValue) value).getSymbol().getMemberNames());
        }
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

    public void defineOperator(Symbol symbol, Operator operator) {
        scope().defineOperator(symbol, operator);
    }

    public void defineValue(Symbol symbol, Type type) {
        scope().defineValue(symbol, type);
    }

    public boolean isOperator(Symbol symbol) {
        return scope().isOperator(symbol);
    }

    public List<DefinitionReference> qualifyDefinitionNames(List<DefinitionReference> references) {
        return references.stream()
            .map(this::getDefinition)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(definition -> definition.qualifyNames(this))
            .map(Definition::getReference)
            .collect(toList());
    }

    public List<Type> qualifyTypeNames(List<Type> types) {
        return types.stream()
            .map(type -> type.qualifyNames(this))
            .collect(toList());
    }

    @SuppressWarnings("unchecked")
    public <T extends Value> List<T> qualifyValueNames(List<T> values) {
        return (List<T>) values.stream()
            .map(value -> value.qualifyNames(this))
            .collect(toList());
    }
}
