package scotch.compiler.parser;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.Definition.scopeDef;
import static scotch.compiler.syntax.DefinitionEntry.entry;
import static scotch.compiler.syntax.DefinitionReference.rootRef;
import static scotch.compiler.syntax.DefinitionReference.scopeRef;
import static scotch.compiler.syntax.SyntaxError.symbolNotFound;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import com.google.common.collect.ImmutableList;
import scotch.compiler.parser.PatternShuffler.ResultVisitor;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.syntax.Definition;
import scotch.compiler.syntax.Definition.DefinitionVisitor;
import scotch.compiler.syntax.Definition.ModuleDefinition;
import scotch.compiler.syntax.Definition.OperatorDefinition;
import scotch.compiler.syntax.Definition.RootDefinition;
import scotch.compiler.syntax.Definition.UnshuffledPattern;
import scotch.compiler.syntax.Definition.ValueDefinition;
import scotch.compiler.syntax.Definition.ValueSignature;
import scotch.compiler.syntax.DefinitionEntry;
import scotch.compiler.syntax.DefinitionGraph;
import scotch.compiler.syntax.DefinitionReference;
import scotch.compiler.syntax.DefinitionReference.DefinitionReferenceVisitor;
import scotch.compiler.syntax.PatternMatch;
import scotch.compiler.syntax.PatternMatcher;
import scotch.compiler.syntax.Scope;
import scotch.compiler.syntax.SyntaxError;
import scotch.compiler.syntax.Value;
import scotch.compiler.syntax.Value.Apply;
import scotch.compiler.syntax.Value.Argument;
import scotch.compiler.syntax.Value.BoolLiteral;
import scotch.compiler.syntax.Value.CharLiteral;
import scotch.compiler.syntax.Value.DoubleLiteral;
import scotch.compiler.syntax.Value.FunctionValue;
import scotch.compiler.syntax.Value.Identifier;
import scotch.compiler.syntax.Value.IntLiteral;
import scotch.compiler.syntax.Value.Message;
import scotch.compiler.syntax.Value.PatternMatchers;
import scotch.compiler.syntax.Value.StringLiteral;
import scotch.compiler.syntax.Value.ValueVisitor;
import scotch.compiler.syntax.builder.SyntaxBuilderFactory;
import scotch.compiler.text.SourceRange;
import scotch.data.either.Either.EitherVisitor;

public class PrecedenceParser implements
    ValueVisitor<Value>,
    DefinitionVisitor<Optional<DefinitionReference>>,
    DefinitionReferenceVisitor<Optional<DefinitionReference>> {

    private final DefinitionGraph                           graph;
    private final SyntaxBuilderFactory                      builderFactory;
    private final Map<DefinitionReference, DefinitionEntry> definitions;
    private final Map<DefinitionReference, Scope>           functionScopes;
    private final PatternShuffler                           patternShuffler;
    private final ValueShuffler                             valueShuffler;
    private final List<SyntaxError>                         errors;
    private final Deque<Scope>                              scopes;

    public PrecedenceParser(DefinitionGraph graph, SyntaxBuilderFactory builderFactory) {
        this.graph = graph;
        this.builderFactory = builderFactory;
        this.definitions = new HashMap<>();
        this.functionScopes = new HashMap<>();
        this.patternShuffler = new PatternShuffler();
        this.valueShuffler = new ValueShuffler(value -> value.accept(this));
        this.errors = new ArrayList<>();
        this.scopes = new ArrayDeque<>();
    }

    public DefinitionGraph parse() {
        rootRef().accept(this);
        return graph
            .copyWith(definitions.values())
            .appendErrors(errors)
            .build();
    }

    @Override
    public Value visit(Apply apply) {
        return apply
            .withFunction(apply.getFunction().accept(this))
            .withArgument(apply.getArgument().accept(this));
    }

    @Override
    public Value visit(BoolLiteral literal) {
        return literal;
    }

    @Override
    public Value visit(CharLiteral literal) {
        return literal;
    }

    @Override
    public Value visit(DoubleLiteral literal) {
        return literal;
    }

    @Override
    public Value visit(FunctionValue function) {
        return scoped(function.getReference(), () -> {
            collect(scopeDef(function));
            return function.withBody(function.getBody().accept(this));
        });
    }

    @Override
    public Value visit(Identifier identifier) {
        if (currentScope().isOperator(identifier.getSymbol())) {
            return currentScope().qualify(identifier.getSymbol())
                .map(identifier::withSymbol)
                .orElseGet(() -> {
                    errors.add(symbolNotFound(identifier.getSymbol(), identifier.getSourceRange()));
                    return identifier;
                });
        } else {
            return identifier;
        }
    }

    @Override
    public Value visit(IntLiteral literal) {
        return literal;
    }

    @Override
    public Value visit(Message message) {
        if (message.getMembers().size() == 1) {
            return message;
        } else {
            return valueShuffler.shuffle(currentScope(), message.getMembers()).accept(new EitherVisitor<SyntaxError, Value, Value>() {
                @Override
                public Value visitLeft(SyntaxError left) {
                    errors.add(left);
                    return message;
                }

                @Override
                public Value visitRight(Value right) {
                    return right;
                }
            });
        }
    }

    @Override
    public Value visit(PatternMatchers matchers) {
        return matchers.withMatchers(matchers.getMatchers().stream()
            .map(this::visitMatcher)
            .collect(toList()));
    }

    @Override
    public Value visit(StringLiteral literal) {
        return literal;
    }

    @Override
    public Optional<DefinitionReference> visit(ValueDefinition definition) {
        return collect(definition.withBody(definition.getBody().accept(this).unwrap()));
    }

    @Override
    public Optional<DefinitionReference> visit(UnshuffledPattern pattern) {
        return patternShuffler.shuffle(currentScope(), pattern).accept(new ResultVisitor<Optional<DefinitionReference>>() {
            @Override
            public Optional<DefinitionReference> error(SyntaxError error) {
                errors.add(error);
                return collect(pattern);
            }

            @Override
            public Optional<DefinitionReference> success(Symbol symbol, List<PatternMatch> matches) {
                PatternMatcher matcher = pattern.asPatternMatcher(matches);
                parentScope().addPattern(symbol, matcher);
                definitions.put(matcher.getReference(), entry(graph.getScope(matcher.getReference()), matcher));
                return Optional.empty();
            }
        });
    }

    @Override
    public Optional<DefinitionReference> visit(ModuleDefinition definition) {
        return collect(definition.withDefinitions(ImmutableList.<DefinitionReference>builder()
            .addAll(definition.getDefinitions().stream()
                .map(reference -> reference.accept(this))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toList()))
            .addAll(processPatterns())
            .build()));
    }

    @Override
    public Optional<DefinitionReference> visit(OperatorDefinition definition) {
        return collect(definition);
    }

    @Override
    public Optional<DefinitionReference> visit(RootDefinition definition) {
        return collect(definition.withDefinitions(definition.getDefinitions().stream()
            .map(reference -> reference.accept(this))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(toList())));
    }

    @Override
    public Optional<DefinitionReference> visit(ValueSignature signature) {
        return collect(signature);
    }

    @Override
    public Optional<DefinitionReference> visitOtherwise(DefinitionReference reference) {
        return scoped(reference, () -> graph.getDefinition(reference)
            .map(definition -> definition.accept(this))
            .filter(Optional::isPresent)
            .map(Optional::get));
    }

    private FunctionValue buildFunction(List<PatternMatcher> patterns, SourceRange sourceRange) {
        Symbol functionSymbol = reserveSymbol();
        List<Argument> arguments = buildFunctionArguments(patterns, sourceRange);
        rebindPatternScope(functionSymbol, patterns);
        FunctionValue function = builderFactory.functionBuilder()
            .withSourceRange(sourceRange)
            .withSymbol(functionSymbol)
            .withArguments(arguments)
            .withBody(builderFactory.patternsBuilder()
                .withSourceRange(sourceRange)
                .withType(reserveType())
                .withPatterns(patterns)
                .build())
            .build();
        collect(scopeDef(function));
        return function;
    }

    private List<Argument> buildFunctionArguments(List<PatternMatcher> patterns, SourceRange sourceRange) {
        int arity = patterns.get(0).getArity();
        List<Argument> arguments = new ArrayList<>();
        for (int i = 0; i < arity; i++) {
            arguments.add(builderFactory.argumentBuilder()
                .withSourceRange(sourceRange)
                .withName("$" + i)
                .withType(reserveType())
                .build());
        }
        return arguments;
    }

    private void rebindPatternScope(Symbol functionSymbol, List<PatternMatcher> patterns) {
        Scope functionScope = currentScope().enterScope();
        functionScopes.put(scopeRef(functionSymbol), functionScope);
        patterns.stream()
            .map(PatternMatcher::getReference)
            .map(graph::getScope)
            .forEach(scope -> scope.setParent(functionScope));
    }

    private Optional<DefinitionReference> collect(Definition definition) {
        definitions.put(definition.getReference(), entry(currentScope(), definition));
        return Optional.of(definition.getReference());
    }

    private Scope currentScope() {
        return scopes.peek();
    }

    private Scope parentScope() {
        return currentScope().getParent();
    }

    private List<DefinitionReference> processPatterns() {
        List<DefinitionReference> members = new ArrayList<>();
        currentScope().getPatterns().forEach((symbol, patterns) -> {
            SourceRange sourceRange = patterns.subList(1, patterns.size()).stream()
                .map(PatternMatcher::getSourceRange)
                .reduce(patterns.get(0).getSourceRange(), SourceRange::extend);
            scoped(() -> builderFactory.valueDefBuilder()
                .withSourceRange(sourceRange)
                .withSymbol(symbol)
                .withType(reserveType())
                .withBody(buildFunction(patterns, sourceRange))
                .build()
                .accept(this)
                .map(members::add));
        });
        return members;
    }

    private Symbol reserveSymbol() {
        return currentScope().reserveSymbol();
    }

    private Type reserveType() {
        return currentScope().reserveType();
    }

    private <T> T scoped(DefinitionReference reference, Supplier<T> supplier) {
        scopes.push(graph.tryGetScope(reference).orElseGet(
            () -> Optional.ofNullable(functionScopes.get(reference)).orElseThrow(
                () -> new IllegalArgumentException("Could not find scope for " + reference))));
        try {
            return supplier.get();
        } finally {
            scopes.pop();
        }
    }

    private <T> T scoped(Supplier<T> supplier) {
        scopes.push(currentScope().enterScope());
        try {
            return supplier.get();
        } finally {
            scopes.pop();
        }
    }

    private PatternMatcher visitMatcher(PatternMatcher matcher) {
        return scoped(matcher.getReference(), () -> {
            collect(scopeDef(matcher));
            List<PatternMatch> matches = new ArrayList<>();
            int counter = 0;
            for (PatternMatch match : matcher.getMatches()) {
                matches.add(match.bind("$" + counter++));
            }
            return matcher
                .withMatches(matches)
                .withBody(matcher.getBody().accept(this).unwrap());
        });
    }
}
