package scotch.compiler.parser;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.Definition.scopeDef;
import static scotch.compiler.syntax.Definition.value;
import static scotch.compiler.syntax.DefinitionEntry.patternEntry;
import static scotch.compiler.syntax.DefinitionEntry.scopedEntry;
import static scotch.compiler.syntax.DefinitionReference.rootRef;
import static scotch.compiler.syntax.DefinitionReference.valueRef;
import static scotch.compiler.syntax.Scope.scope;
import static scotch.compiler.syntax.SyntaxError.symbolNotFound;
import static scotch.compiler.syntax.Value.arg;
import static scotch.compiler.syntax.Value.fn;
import static scotch.compiler.syntax.Value.patterns;
import static scotch.compiler.text.SourceRange.NULL_SOURCE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import com.google.common.collect.ImmutableList;
import scotch.compiler.parser.PatternShuffler.ResultVisitor;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.SymbolGenerator;
import scotch.compiler.symbol.SymbolResolver;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.Type.FunctionType;
import scotch.compiler.symbol.Type.SumType;
import scotch.compiler.symbol.Type.TypeVisitor;
import scotch.compiler.syntax.Definition;
import scotch.compiler.syntax.Definition.DefinitionVisitor;
import scotch.compiler.syntax.Definition.ModuleDefinition;
import scotch.compiler.syntax.Definition.OperatorDefinition;
import scotch.compiler.syntax.Definition.RootDefinition;
import scotch.compiler.syntax.Definition.UnshuffledPattern;
import scotch.compiler.syntax.Definition.ValueDefinition;
import scotch.compiler.syntax.Definition.ValueSignature;
import scotch.compiler.syntax.DefinitionEntry;
import scotch.compiler.syntax.DefinitionEntry.DefinitionEntryVisitor;
import scotch.compiler.syntax.DefinitionGraph;
import scotch.compiler.syntax.DefinitionReference;
import scotch.compiler.syntax.DefinitionReference.DefinitionReferenceVisitor;
import scotch.compiler.syntax.DefinitionReference.ModuleReference;
import scotch.compiler.syntax.Import;
import scotch.compiler.syntax.PatternMatch;
import scotch.compiler.syntax.PatternMatch.CaptureMatch;
import scotch.compiler.syntax.PatternMatch.EqualMatch;
import scotch.compiler.syntax.PatternMatch.PatternMatchVisitor;
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
import scotch.compiler.text.SourceRange;
import scotch.data.either.Either.EitherVisitor;

public class SyntaxParser implements
    DefinitionReferenceVisitor<Optional<DefinitionReference>>,
    DefinitionVisitor<Optional<DefinitionReference>>,
    DefinitionEntryVisitor<DefinitionEntry>,
    ValueVisitor<Value>,
    PatternMatchVisitor<PatternMatch>,
    TypeVisitor<Type> {

    private final DefinitionGraph                           graph;
    private final PatternShuffler                           patternShuffler;
    private final ValueShuffler                             valueShuffler;
    private final List<SyntaxError>                         errors;
    private final SymbolGenerator                           symbolGenerator;
    private final Map<DefinitionReference, DefinitionEntry> definitions;
    private       Scope                                     scope;
    private       String                                    currentModule;

    public SyntaxParser(DefinitionGraph graph, SymbolResolver resolver) {
        this.graph = graph;
        this.patternShuffler = new PatternShuffler();
        this.valueShuffler = new ValueShuffler(value -> value.accept(this));
        this.errors = new ArrayList<>();
        this.symbolGenerator = graph.getSymbolGenerator();
        this.scope = scope(symbolGenerator, resolver);
        this.definitions = new HashMap<>();
    }

    public DefinitionGraph analyze() {
        graph.getDefinition(rootRef()).ifPresent(root -> root.accept(this));
        return graph
            .copyWith(ImmutableList.copyOf(definitions.values()))
            .withSequence(symbolGenerator)
            .appendErrors(errors)
            .build();
    }

    @Override
    public Value visit(Argument argument) {
        scope.defineValue(argument.getSymbol(), argument.getType());
        return argument;
    }

    @Override
    public Optional<DefinitionReference> visit(ModuleDefinition definition) {
        return scoped(
            definition.getImports(),
            () -> Optional.of(collect(definition.withDefinitions(mapDefinitions(definition.getDefinitions()))))
        );
    }

    @Override
    public Optional<DefinitionReference> visit(ModuleReference reference) {
        currentModule = reference.getName();
        return parseDefinition(reference);
    }

    @Override
    public Optional<DefinitionReference> visit(OperatorDefinition definition) {
        scope.defineOperator(definition.getSymbol(), definition.getOperator());
        return Optional.of(collect(definition));
    }

    @Override
    public Optional<DefinitionReference> visit(RootDefinition definition) {
        return Optional.of(collect(definition.withDefinitions(mapDefinitions(definition.getDefinitions()))));
    }

    @Override
    public Optional<DefinitionReference> visit(UnshuffledPattern pattern) {
        return patternShuffler.shuffle(scope, pattern).accept(new ResultVisitor<Optional<DefinitionReference>>() {
            @Override
            public Optional<DefinitionReference> error(SyntaxError error) {
                errors.add(error);
                return Optional.empty();
            }

            @Override
            public Optional<DefinitionReference> success(Symbol symbol, List<PatternMatch> matches) {
                return appendPattern(symbol, pattern.asPatternMatcher(bind(matches)));
            }

            private List<PatternMatch> bind(List<PatternMatch> matches) {
                AtomicInteger atom = new AtomicInteger();
                return matches.stream()
                    .map(match -> match.bind("$" + atom.getAndIncrement()))
                    .collect(toList());
            }
        });
    }

    @Override
    public PatternMatch visit(EqualMatch match) {
        return match;
    }

    @Override
    public Value visit(FunctionValue function) {
        return scoped(() -> {
            try {
                function.getArguments().forEach(arg -> arg.accept(this));
                return function.withBody(function.getBody().accept(this));
            } finally {
                collect(scopeDef(function.getSourceRange(), function.getSymbol()));
            }
        });
    }

    @Override
    public Value visit(Identifier identifier) {
        return scope.qualify(identifier.getSymbol())
            .map(identifier::withSymbol)
            .orElseGet(() -> {
                errors.add(symbolNotFound(identifier.getSymbol(), identifier.getSourceRange()));
                return identifier;
            });
    }

    @Override
    public Value visit(PatternMatchers matchers) {
        return matchers.withMatchers(
            matchers.getMatchers().stream()
                .map(this::visitMatcher)
                .collect(toList())
        );
    }

    @Override
    public PatternMatch visit(CaptureMatch match) {
        scope.defineValue(match.getSymbol(), match.getType());
        scope.specialize(match.getType());
        return match;
    }

    @Override
    public Optional<DefinitionReference> visit(ValueDefinition definition) {
        scope.defineValue(definition.getSymbol(), definition.getType());
        scope.specialize(definition.getType());
        return scoped(() -> Optional.of(collect(definition.withBody(definition.getBody().accept(this).unwrap()))));
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
    public Value visit(IntLiteral literal) {
        return literal;
    }

    @Override
    public Value visit(StringLiteral literal) {
        return literal;
    }

    @Override
    public Value visit(Message message) {
        return valueShuffler.shuffle(scope, message.getMembers()).accept(new EitherVisitor<SyntaxError, Value, Value>() {
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

    @Override
    public Optional<DefinitionReference> visit(ValueSignature signature) {
        scope.defineSignature(signature.getSymbol(), signature.getType().accept(this));
        return Optional.empty();
    }

    @Override
    public Type visit(SumType type) {
        return scope.qualify(type.getSymbol())
            .<Type>map(type::withSymbol)
            .orElseGet(() -> {
                errors.add(symbolNotFound(type.getSymbol(), type.getSourceRange()));
                return scope.reserveType();
            });
    }

    @Override
    public Type visit(FunctionType type) {
        return type.withArgument(type.getArgument().accept(this)).withResult(type.getResult().accept(this));
    }

    @Override
    public Optional<DefinitionReference> visitOtherwise(DefinitionReference reference) {
        return parseDefinition(reference);
    }

    private Optional<DefinitionReference> appendPattern(Symbol symbol, PatternMatcher pattern) {
        DefinitionReference reference = null;
        if (!scope.isPattern(symbol)) {
            scope.beginPattern(symbol);
            reference = valueRef(symbol);
        }
        scope.addPattern(symbol, pattern);
        return Optional.ofNullable(reference);
    }

    private DefinitionReference collect(Definition definition) {
        definitions.put(definition.getReference(), scopedEntry(definition, scope));
        return definition.getReference();
    }

    private PatternMatcher collect(PatternMatcher pattern) {
        definitions.put(pattern.getReference(), patternEntry(pattern, scope));
        return pattern;
    }

    private void consolidatePatterns() {
        scope.getPatterns().forEach((symbol, patterns) -> {
            SourceRange sourceRange = patterns.stream()
                .map(PatternMatcher::getSourceRange)
                .reduce(NULL_SOURCE, SourceRange::extend);
            SourceRange startRange = sourceRange.getStartRange();
            int arity = patterns.get(0).getArity();
            List<Argument> arguments = new ArrayList<>();
            for (int i = 0; i < arity; i++) {
                arguments.add(arg(startRange, "$" + i, scope.reserveType()));
            }
            Symbol function = symbolGenerator.reserveSymbol(currentModule);
            collect(scopeDef(sourceRange, function));
            value(sourceRange, symbol, scope.reserveType(), fn(
                startRange,
                function,
                arguments,
                patterns(sourceRange, scope.reserveType(), patterns)
            )).accept(this);
        });
    }

    private List<DefinitionReference> mapDefinitions(List<DefinitionReference> definitions) {
        return definitions.stream()
            .map(reference -> reference.accept(this))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(toList());
    }

    private Optional<DefinitionReference> parseDefinition(DefinitionReference reference) {
        return graph.getDefinition(reference).flatMap(definition -> definition.accept(this));
    }

    private <T> T scoped(Optional<List<Import>> imports, Supplier<T> supplier) {
        scope = imports.map(i -> scope.enterScope(currentModule, i)).orElseGet(scope::enterScope);
        try {
            return supplier.get();
        } finally {
            consolidatePatterns();
            scope = scope.leaveScope();
        }
    }

    private <T> T scoped(List<Import> imports, Supplier<T> supplier) {
        return scoped(Optional.of(imports), supplier);
    }

    private <T> T scoped(Supplier<T> supplier) {
        return scoped(Optional.empty(), supplier);
    }

    private PatternMatcher visitMatcher(PatternMatcher matcher) {
        return scoped(() -> collect(matcher
            .withMatches(matcher.getMatches().stream().map(match -> match.accept(this)).collect(toList()))
            .withBody(matcher.getBody().accept(this).unwrap())));
    }
}
