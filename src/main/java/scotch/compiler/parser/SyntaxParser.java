package scotch.compiler.parser;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.Definition.value;
import static scotch.compiler.syntax.DefinitionEntry.patternEntry;
import static scotch.compiler.syntax.DefinitionEntry.scopedEntry;
import static scotch.compiler.syntax.DefinitionReference.rootRef;
import static scotch.compiler.syntax.DefinitionReference.valueRef;
import static scotch.compiler.syntax.Scope.scope;
import static scotch.compiler.syntax.SyntaxError.symbolNotFound;
import static scotch.compiler.syntax.Value.emptyPatterns;
import static scotch.compiler.text.SourceRange.NULL_SOURCE;
import static scotch.data.tuple.TupleValues.tuple2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import com.google.common.collect.ImmutableList;
import scotch.compiler.parser.PatternShuffler.ResultVisitor;
import scotch.compiler.symbol.Symbol;
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
import scotch.compiler.syntax.DefinitionEntry.ScopedEntry;
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
import scotch.compiler.syntax.TypeGenerator;
import scotch.compiler.syntax.Value;
import scotch.compiler.syntax.Value.Apply;
import scotch.compiler.syntax.Value.Identifier;
import scotch.compiler.syntax.Value.LiteralValue;
import scotch.compiler.syntax.Value.Message;
import scotch.compiler.syntax.Value.PatternMatchers;
import scotch.compiler.syntax.Value.ValueVisitor;
import scotch.data.either.Either.EitherVisitor;
import scotch.data.tuple.Tuple2;

public class SyntaxParser implements
    DefinitionReferenceVisitor<Optional<DefinitionReference>>,
    DefinitionVisitor<Optional<Definition>>,
    DefinitionEntryVisitor<DefinitionEntry>,
    ValueVisitor<Value>,
    PatternMatchVisitor<PatternMatch>,
    TypeVisitor<Type> {

    private final DefinitionGraph                           graph;
    private final PatternShuffler                           patternShuffler;
    private final ValueShuffler                             valueShuffler;
    private final List<SyntaxError>                         errors;
    private final TypeGenerator                             typeGenerator;
    private final Map<DefinitionReference, DefinitionEntry> definitions;
    private       Scope                                     scope;
    private       String                                    currentModule;

    public SyntaxParser(DefinitionGraph graph, SymbolResolver resolver) {
        this.graph = graph;
        this.patternShuffler = new PatternShuffler();
        this.valueShuffler = new ValueShuffler(value -> value.accept(this));
        this.errors = new ArrayList<>();
        this.typeGenerator = graph.getTypeGenerator();
        this.scope = scope(typeGenerator, resolver);
        this.definitions = new HashMap<>();
    }

    public DefinitionGraph analyze() {
        // TODO: results in "empty" symbol table if root not present
        graph.getDefinition(rootRef()).ifPresent(root -> root.accept(this));
        return graph
            .copyWith(ImmutableList.copyOf(definitions.values()))
            .withSequence(typeGenerator)
            .withErrors(errors)
            .build();
    }

    @Override
    public Optional<Definition> visit(ModuleDefinition definition) {
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
    public Optional<Definition> visit(OperatorDefinition definition) {
        scope.defineOperator(definition.getSymbol(), definition.getOperator());
        return Optional.of(collect(definition));
    }

    @Override
    public Optional<Definition> visit(RootDefinition definition) {
        return Optional.of(collect(definition.withDefinitions(mapDefinitions(definition.getDefinitions()))));
    }

    @Override
    public Optional<Definition> visit(UnshuffledPattern pattern) {
        return patternShuffler.shuffle(scope, pattern).accept(new ResultVisitor<Optional<Definition>>() {
            @Override
            public Optional<Definition> error(SyntaxError error) {
                errors.add(error);
                return Optional.empty();
            }

            @Override
            public Optional<Definition> success(Symbol symbol, List<PatternMatch> matches) {
                return appendPattern(symbol, reference -> replaceDefinitionValue(reference, definition -> new ValueVisitor<ValueDefinition>() {
                    @Override
                    public ValueDefinition visit(PatternMatchers matchers) {
                        return definition
                            .withSourceRange(matchers.getSourceRange().extend(pattern.getSourceRange()))
                            .withBody(matchers.withMatchers(ImmutableList.<PatternMatcher>builder()
                                .addAll(matchers.getMatchers())
                                .add(visitMatcher(pattern.asPatternMatcher(matches)))
                                .build()));
                    }
                }));
            }
        });
    }

    @Override
    public PatternMatch visit(EqualMatch match) {
        return match;
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
        return match;
    }

    @Override
    public Optional<Definition> visit(ValueDefinition definition) {
        scope.defineValue(definition.getSymbol(), definition.getType());
        return scoped(() -> Optional.of(collect(definition.withBody(unwrap(definition.getBody().accept(this))))));
    }

    @Override
    public Value visit(Apply apply) {
        return apply
            .withFunction(apply.getFunction().accept(this))
            .withArgument(apply.getArgument().accept(this));
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
    public Value visit(LiteralValue literal) {
        return literal;
    }

    @Override
    public Optional<Definition> visit(ValueSignature signature) {
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

    private Optional<Definition> appendPattern(Symbol symbol, Consumer<DefinitionReference> consumer) {
        return (scope.isPattern(symbol) ? retrievePattern(symbol) : createPattern(symbol)).into(
            (optionalDefinition, reference) -> {
                consumer.accept(reference);
                return optionalDefinition;
            }
        );
    }

    private Definition collect(Definition definition) {
        definitions.put(definition.getReference(), scopedEntry(definition, scope));
        return definition;
    }

    private PatternMatcher collect(PatternMatcher pattern) {
        definitions.put(pattern.getReference(), patternEntry(pattern, scope));
        return pattern;
    }

    private Tuple2<Optional<Definition>, DefinitionReference> createPattern(Symbol symbol) {
        Type type = tryGetType(symbol);
        Definition definition = collect(value(NULL_SOURCE, symbol, type, emptyPatterns(scope.reserveType())));
        scope.defineValue(symbol, type);
        scope.addPattern(symbol);
        return tuple2(Optional.of(definition), definition.getReference());
    }

    private List<DefinitionReference> mapDefinitions(List<DefinitionReference> definitions) {
        return definitions.stream()
            .map(reference -> reference.accept(this))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(toList());
    }

    private Optional<DefinitionReference> parseDefinition(DefinitionReference reference) {
        return graph.getDefinition(reference)
            .flatMap(definition -> definition.accept(this))
            .map(Definition::getReference);
    }

    private void replaceDefinitionValue(DefinitionReference reference, Function<ValueDefinition, ValueVisitor<ValueDefinition>> function) {
        ScopedEntry entry = (ScopedEntry) definitions.get(reference);
        definitions.put(reference, entry.withDefinition(entry.getDefinition().accept(new DefinitionVisitor<Definition>() {
            @Override
            public Definition visit(ValueDefinition definition) {
                return definition.getBody().accept(function.apply(definition));
            }
        })));
    }

    private Tuple2<Optional<Definition>, DefinitionReference> retrievePattern(Symbol symbol) {
        return tuple2(Optional.empty(), valueRef(symbol));
    }

    private <T> T scoped(Optional<List<Import>> imports, Supplier<T> supplier) {
        scope = imports.map(i -> scope.enterScope(currentModule, i)).orElseGet(scope::enterScope);
        try {
            return supplier.get();
        } finally {
            scope = scope.leaveScope();
        }
    }

    private <T> T scoped(List<Import> imports, Supplier<T> supplier) {
        return scoped(Optional.of(imports), supplier);
    }

    private <T> T scoped(Supplier<T> supplier) {
        return scoped(Optional.empty(), supplier);
    }

    private Type tryGetType(Symbol symbol) {
        return scope.getSignature(symbol).orElseGet(scope::reserveType);
    }

    private Value unwrap(Value value) {
        return value.accept(new ValueVisitor<Value>() {
            @Override
            public Value visit(Message message) {
                if (message.getMembers().size() == 1) {
                    return unwrap(message.getMembers().get(0));
                } else {
                    throw new UnsupportedOperationException(); // TODO
                }
            }

            @Override
            public Value visit(Apply apply) {
                return apply
                    .withFunction(unwrap(apply.getFunction()))
                    .withArgument(unwrap(apply.getArgument()));
            }

            @Override
            public Value visit(PatternMatchers matchers) {
                return matchers.withMatchers(
                    matchers.getMatchers().stream()
                        .map(matcher -> matcher.withBody(unwrap(matcher.getBody())))
                        .collect(toList())
                );
            }

            @Override
            public Value visit(Identifier identifier) {
                return identifier;
            }

            @Override
            public Value visit(LiteralValue literal) {
                return literal;
            }

            @Override
            public Value visitOtherwise(Value value) {
                throw new UnsupportedOperationException(); // TODO
            }
        });
    }

    private PatternMatcher visitMatcher(PatternMatcher matcher) {
        return scoped(() -> collect(matcher
                .withMatches(matcher.getMatches().stream().map(match -> match.accept(this)).collect(toList()))
                .withBody(unwrap(matcher.getBody().accept(this)))
        ));
    }
}
