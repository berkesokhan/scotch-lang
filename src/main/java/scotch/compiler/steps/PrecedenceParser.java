package scotch.compiler.steps;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.definition.DefinitionEntry.entry;
import static scotch.compiler.syntax.definition.Definitions.scopeDef;
import static scotch.compiler.syntax.reference.DefinitionReference.rootRef;
import static scotch.compiler.syntax.reference.DefinitionReference.valueRef;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import com.google.common.collect.ImmutableList;
import scotch.compiler.error.SymbolNotFoundError;
import scotch.compiler.error.SyntaxError;
import scotch.symbol.Operator;
import scotch.symbol.Symbol;
import scotch.symbol.type.Type;
import scotch.compiler.syntax.Scoped;
import scotch.compiler.syntax.definition.Definition;
import scotch.compiler.syntax.definition.DefinitionEntry;
import scotch.compiler.syntax.definition.DefinitionGraph;
import scotch.compiler.syntax.definition.UnshuffledDefinition;
import scotch.compiler.syntax.definition.ValueDefinition;
import scotch.compiler.syntax.pattern.PatternCase;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.syntax.value.Argument;
import scotch.compiler.syntax.value.PatternMatcher;
import scotch.compiler.syntax.value.UnshuffledValue;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.text.SourceLocation;

public class PrecedenceParser {

    private final DefinitionGraph                           graph;
    private final Deque<Scope>                              scopes;
    private final Map<DefinitionReference, Scope>           patternScopes;
    private final Map<DefinitionReference, DefinitionEntry> entries;
    private final Deque<List<String>>                       memberNames;
    private final List<SyntaxError>                         errors;

    public PrecedenceParser(DefinitionGraph graph) {
        this.graph = graph;
        this.scopes = new ArrayDeque<>();
        this.patternScopes = new HashMap<>();
        this.entries = new HashMap<>();
        this.memberNames = new ArrayDeque<>(asList(ImmutableList.of()));
        this.errors = new ArrayList<>();
    }

    public void addPattern(Symbol symbol, PatternCase matcher) {
        scope().getParent().addPattern(symbol, matcher);
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

    public List<DefinitionReference> map(List<DefinitionReference> references, BiFunction<? super Definition, PrecedenceParser, ? extends Definition> function) {
        return references.stream()
            .map(this::getDefinition)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(definition -> function.apply(definition, this))
            .map(Definition::getReference)
            .collect(toList());
    }

    public List<DefinitionReference> mapOptional(List<DefinitionReference> references, BiFunction<? super Definition, PrecedenceParser, Optional<? extends Definition>> function) {
        return references.stream()
            .map(this::getDefinition)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(definition -> function.apply(definition, this))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(Definition::getReference)
            .collect(toList());
    }

    public <T> T named(Symbol symbol, Supplier<? extends T> supplier) {
        memberNames.push(symbol.getMemberNames());
        T result = supplier.get();
        memberNames.pop();
        return result;
    }

    public DefinitionGraph parsePrecedence() {
        Definition root = getDefinition(rootRef()).orElseThrow(() -> new IllegalStateException("No root found!"));
        scopedOptional(root, () -> root.parsePrecedence(this));
        return graph
            .copyWith(entries.values())
            .appendErrors(errors)
            .build();
    }

    public List<DefinitionReference> processPatterns() {
        List<DefinitionReference> members = new ArrayList<>();
        scope().getPatternCases().forEach((symbol, patternCases) -> {
            SourceLocation sourceLocation = patternCases.subList(1, patternCases.size()).stream()
                .map(PatternCase::getSourceLocation)
                .reduce(patternCases.get(0).getSourceLocation(), SourceLocation::extend);
            PatternMatcher function = buildMatcher(patternCases, sourceLocation);

            patternCases.stream()
                .map(this::collect)
                .map(Definition::getReference)
                .map(this::getScope)
                .forEach(scope -> scope.setParent(getScope(function.getReference())));

            Scope scope = scope().enterScope();
            patternScopes.put(valueRef(symbol), scope);
            getScope(function.getReference()).setParent(scope);
            ValueDefinition.builder()
                .withSourceLocation(sourceLocation)
                .withSymbol(symbol)
                .withBody(function)
                .build()
                .parsePrecedence(this)
                .map(Definition::getReference)
                .map(members::add);
            patternScopes.remove(valueRef(symbol));
        });
        return members;
    }

    public Optional<Symbol> qualify(Symbol symbol) {
        return scope().qualify(symbol);
    }

    public Symbol reserveSymbol() {
        return scope().reserveSymbol(memberNames.peek());
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

    public <T extends Definition> Optional<Definition> scopedOptional(T definition, Supplier<Optional<? extends T>> supplier) {
        enterScope(definition);
        try {
            return supplier.get().map(this::collect);
        } finally {
            leaveScope();
        }
    }

    public Optional<Definition> shuffle(UnshuffledDefinition pattern) {
        return new PatternShuffler().shuffle(scope(), memberNames.peek(), pattern)
            .map(r -> {
                addPattern(r.getSymbol(), pattern.asPatternMatcher(r.getMatches()));
                return Optional.<Definition>empty();
            })
            .orElseGet(error -> {
                errors.add(error);
                return Optional.of((Definition) pattern);
            });
    }

    public Value shuffle(UnshuffledValue value) {
        return new ValueShuffler(v -> v.parsePrecedence(this))
            .shuffle(scope(), value.getValues()).orElseGet(left -> {
                error(left);
                return value;
            });
    }

    public void symbolNotFound(Symbol symbol, SourceLocation sourceLocation) {
        errors.add(SymbolNotFoundError.symbolNotFound(symbol, sourceLocation));
    }

    private PatternMatcher buildMatcher(List<PatternCase> patternCases, SourceLocation sourceLocation) {
        List<Argument> arguments = buildFunctionArguments(patternCases, sourceLocation);
        PatternMatcher matcher = PatternMatcher.builder()
            .withSourceLocation(sourceLocation)
            .withSymbol(scope().reserveSymbol(ImmutableList.of()))
            .withType(scope().reserveType())
            .withArguments(arguments)
            .withPatterns(patternCases)
            .build();
        patternScopes.put(matcher.getReference(), scope().enterScope());
        return matcher;
    }

    private List<Argument> buildFunctionArguments(List<PatternCase> patterns, SourceLocation sourceLocation) {
        int arity = patterns.get(0).getArity();
        List<Argument> arguments = new ArrayList<>();
        for (int i = 0; i < arity; i++) {
            arguments.add(Argument.builder()
                .withSourceLocation(sourceLocation)
                .withName("#" + i)
                .withType(scope().reserveType())
                .build());
        }
        return arguments;
    }

    private Definition collect(Definition definition) {
        entries.put(definition.getReference(), entry(scope(), definition));
        return definition;
    }

    private Definition collect(PatternCase pattern) {
        return collect(scopeDef(pattern));
    }

    private void enterScope(DefinitionReference reference) {
        scopes.push(getScope(reference));
    }

    private Scope getScope(DefinitionReference reference) {
        return graph.tryGetScope(reference).orElseGet(
            () -> Optional.ofNullable(patternScopes.get(reference)).orElseGet(
                () -> entries.get(reference).getScope()));
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
}
