package scotch.compiler;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.definition.DefinitionEntry.entry;
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
import scotch.compiler.error.SyntaxError;
import scotch.compiler.parser.PatternShuffler;
import scotch.compiler.parser.PatternShuffler.ResultVisitor;
import scotch.compiler.parser.ValueShuffler;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.SymbolNotFoundError;
import scotch.compiler.syntax.PrecedenceParser;
import scotch.compiler.syntax.Scoped;
import scotch.compiler.syntax.builder.SyntaxBuilderFactory;
import scotch.compiler.syntax.definition.Definition;
import scotch.compiler.syntax.definition.DefinitionEntry;
import scotch.compiler.syntax.definition.DefinitionGraph;
import scotch.compiler.syntax.definition.UnshuffledPattern;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.syntax.value.Argument;
import scotch.compiler.syntax.value.FunctionValue;
import scotch.compiler.syntax.value.PatternMatch;
import scotch.compiler.syntax.value.PatternMatcher;
import scotch.compiler.syntax.value.UnshuffledValue;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.text.SourceRange;

public class PrecedenceParserState implements PrecedenceParser {

    private final DefinitionGraph                           graph;
    private final SyntaxBuilderFactory                      builderFactory;
    private final Deque<Scope>                              scopes;
    private final Map<DefinitionReference, Scope>           functionScopes;
    private final Map<DefinitionReference, DefinitionEntry> entries;
    private final List<SyntaxError>                         errors;

    public PrecedenceParserState(DefinitionGraph graph) {
        this.graph = graph;
        this.builderFactory = new SyntaxBuilderFactory();
        this.scopes = new ArrayDeque<>();
        this.functionScopes = new HashMap<>();
        this.entries = new HashMap<>();
        this.errors = new ArrayList<>();
    }

    @Override
    public void addPattern(Symbol symbol, PatternMatcher matcher) {
        scope().getParent().addPattern(symbol, matcher);
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

    @Override
    public DefinitionGraph getGraph() {
        return graph
            .copyWith(entries.values())
            .appendErrors(errors)
            .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Scoped> T keep(Scoped scoped) {
        return (T) scoped(scoped, () -> scoped);
    }

    @Override
    public void leaveScope() {
        scopes.pop();
    }

    @Override
    public List<DefinitionReference> map(List<DefinitionReference> references, BiFunction<? super Definition, PrecedenceParser, ? extends Definition> function) {
        return references.stream()
            .map(this::getDefinition)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(definition -> function.apply(definition, this))
            .map(Definition::getReference)
            .collect(toList());
    }

    @Override
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

    @Override
    public void parsePrecedence() {
        Definition root = getDefinition(rootRef()).orElseThrow(() -> new IllegalStateException("No root found!"));
        scopedOptional(root, () -> root.parsePrecedence(this));
    }

    @Override
    public List<DefinitionReference> processPatterns() {
        List<DefinitionReference> members = new ArrayList<>();
        scope().getPatterns().forEach((symbol, patterns) -> {
            SourceRange sourceRange = patterns.subList(1, patterns.size()).stream()
                .map(PatternMatcher::getSourceRange)
                .reduce(patterns.get(0).getSourceRange(), SourceRange::extend);
            FunctionValue function = buildFunction(patterns, sourceRange);

            patterns.stream()
                .map(this::collect)
                .map(Definition::getReference)
                .map(this::getScope)
                .forEach(scope -> scope.bind(getScope(function.getReference())));

            Scope scope = scope().enterScope();
            functionScopes.put(valueRef(symbol), scope);
            getScope(function.getReference()).bind(scope);
            builderFactory.valueDefBuilder()
                .withSourceRange(sourceRange)
                .withSymbol(symbol)
                .withType(scope().reserveType())
                .withBody(function)
                .build()
                .parsePrecedence(this)
                .map(Definition::getReference)
                .map(members::add);
            functionScopes.remove(valueRef(symbol));
        });
        return members;
    }

    @Override
    public Optional<Symbol> qualify(Symbol symbol) {
        return scope().qualify(symbol);
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

    @Override
    public <T extends Definition> Optional<Definition> scopedOptional(T definition, Supplier<Optional<? extends T>> supplier) {
        enterScope(definition);
        try {
            return supplier.get().map(this::collect);
        } finally {
            leaveScope();
        }
    }

    @Override
    public Optional<Definition> shuffle(UnshuffledPattern pattern) {
        return new PatternShuffler().shuffle(scope(), pattern)
            .accept(new ResultVisitor<Optional<Definition>>() {
                @Override
                public Optional<Definition> error(SyntaxError error) {
                    errors.add(error);
                    return Optional.of(pattern);
                }

                @Override
                public Optional<Definition> success(Symbol symbol, List<PatternMatch> matches) {
                    addPattern(symbol, pattern.asPatternMatcher(matches));
                    return Optional.empty();
                }
            });
    }

    @Override
    public Value shuffle(UnshuffledValue value) {
        return new ValueShuffler(v -> v.parsePrecedence(this))
            .shuffle(scope(), value.getValues()).getRightOr(left -> {
                error(left);
                return value;
            });
    }

    @Override
    public void symbolNotFound(Symbol symbol, SourceRange sourceRange) {
        errors.add(SymbolNotFoundError.symbolNotFound(symbol, sourceRange));
    }

    private FunctionValue buildFunction(List<PatternMatcher> patterns, SourceRange sourceRange) {
        Symbol functionSymbol = scope().reserveSymbol();
        List<Argument> arguments = buildFunctionArguments(patterns, sourceRange);
        FunctionValue function = builderFactory.functionBuilder()
            .withSourceRange(sourceRange)
            .withSymbol(functionSymbol)
            .withArguments(arguments)
            .withBody(builderFactory.patternsBuilder()
                .withSourceRange(sourceRange)
                .withType(scope().reserveType())
                .withPatterns(patterns)
                .build())
            .build();
        functionScopes.put(function.getReference(), scope().enterScope());
        return function;
    }

    private List<Argument> buildFunctionArguments(List<PatternMatcher> patterns, SourceRange sourceRange) {
        int arity = patterns.get(0).getArity();
        List<Argument> arguments = new ArrayList<>();
        for (int i = 0; i < arity; i++) {
            arguments.add(builderFactory.argumentBuilder()
                .withSourceRange(sourceRange)
                .withName("$" + i)
                .withType(scope().reserveType())
                .build());
        }
        return arguments;
    }

    private void enterScope(DefinitionReference reference) {
        scopes.push(getScope(reference));
    }

    private Scope getScope(DefinitionReference reference) {
        return graph.tryGetScope(reference).orElseGet(() -> functionScopes.get(reference));
    }
}
