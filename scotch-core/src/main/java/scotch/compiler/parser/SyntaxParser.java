package scotch.compiler.parser;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.DefinitionReference.rootRef;
import static scotch.compiler.syntax.Scope.symbolNotFound;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
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
import scotch.compiler.syntax.DefinitionReference;
import scotch.compiler.syntax.DefinitionReference.DefinitionReferenceVisitor;
import scotch.compiler.syntax.DefinitionReference.ModuleReference;
import scotch.compiler.syntax.Import;
import scotch.compiler.syntax.PatternMatch;
import scotch.compiler.syntax.PatternMatch.CaptureMatch;
import scotch.compiler.syntax.PatternMatch.EqualMatch;
import scotch.compiler.syntax.PatternMatch.PatternMatchVisitor;
import scotch.compiler.syntax.PatternMatcher;
import scotch.compiler.syntax.SymbolResolver;
import scotch.compiler.syntax.SymbolTable;
import scotch.compiler.syntax.Type;
import scotch.compiler.syntax.Type.FunctionType;
import scotch.compiler.syntax.Type.SumType;
import scotch.compiler.syntax.Type.TypeVisitor;
import scotch.compiler.syntax.Value;
import scotch.compiler.syntax.Value.Apply;
import scotch.compiler.syntax.Value.Identifier;
import scotch.compiler.syntax.Value.LiteralValue;
import scotch.compiler.syntax.Value.Message;
import scotch.compiler.syntax.Value.PatternMatchers;
import scotch.compiler.syntax.Value.ValueVisitor;

public class SyntaxParser implements
    DefinitionReferenceVisitor<Optional<DefinitionReference>>,
    DefinitionVisitor<Optional<Definition>>,
    DefinitionEntryVisitor<DefinitionEntry>,
    ValueVisitor<Value>,
    PatternMatchVisitor<PatternMatch>,
    TypeVisitor<Type> {

    private final SymbolTable     symbols;
    private final ScopeBuilder    scope;
    private final PatternShuffler patternShuffler;
    private final ValueShuffler   valueShuffler;

    public SyntaxParser(SymbolTable symbols, SymbolResolver resolver) {
        this.symbols = symbols;
        this.scope = new ScopeBuilder(symbols.getSequence(), resolver);
        this.patternShuffler = new PatternShuffler(scope, this::visitMatcher);
        this.valueShuffler = new ValueShuffler(scope, value -> value.accept(this));
    }

    public SymbolTable analyze() {
        symbols.getDefinition(rootRef()).accept(this);
        return symbols
            .copyWith(scope.getDefinitions())
            .withSequence(scope.getSequence())
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
        scope.setCurrentModule(reference.getName());
        return parseDefinition(reference);
    }

    @Override
    public Optional<Definition> visit(OperatorDefinition definition) {
        scope.defineOperator(definition.getSymbol(), definition);
        return Optional.of(collect(definition));
    }

    @Override
    public Optional<Definition> visit(RootDefinition definition) {
        return Optional.of(collect(definition.withDefinitions(mapDefinitions(definition.getDefinitions()))));
    }

    @Override
    public Optional<Definition> visit(UnshuffledPattern pattern) {
        return patternShuffler.shuffle(pattern);
    }

    @Override
    public PatternMatch visit(EqualMatch match) {
        return match;
    }

    @Override
    public Value visit(Identifier identifier) {
        return scope.qualify(identifier.getSymbol())
            .map(identifier::withSymbol)
            .orElseThrow(() -> symbolNotFound(identifier.getSymbol()));
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
        return shuffleMessage(message.getMembers());
    }

    @Override
    public Value visit(LiteralValue value) {
        return value;
    }

    @Override
    public Optional<Definition> visit(ValueSignature signature) {
        scope.defineSignature(signature.getSymbol(), signature.getType().accept(this));
        return Optional.empty();
    }

    @Override
    public Type visit(SumType type) {
        return type.withSymbol(scope.qualify(type.getSymbol()).orElseThrow(() -> symbolNotFound(type.getSymbol())));
    }

    @Override
    public Type visit(FunctionType type) {
        return type.withArgument(type.getArgument().accept(this)).withResult(type.getResult().accept(this));
    }

    @Override
    public PatternMatch visitOtherwise(PatternMatch match) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public Value visitOtherwise(Value value) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public Optional<Definition> visitOtherwise(Definition definition) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public Type visitOtherwise(Type type) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public Optional<DefinitionReference> visitOtherwise(DefinitionReference reference) {
        return parseDefinition(reference);
    }

    private Definition collect(Definition definition) {
        return scope.collect(definition);
    }

    private PatternMatcher collect(PatternMatcher pattern) {
        return scope.collect(pattern);
    }

    private List<DefinitionReference> mapDefinitions(List<DefinitionReference> definitions) {
        return definitions.stream()
            .map(reference -> reference.accept(this))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(toList());
    }

    private Optional<DefinitionReference> parseDefinition(DefinitionReference reference) {
        return symbols.getDefinition(reference).accept(this).map(Definition::getReference);
    }

    private <T> T scoped(List<Import> imports, Supplier<T> supplier) {
        scope.enterScope(imports);
        try {
            return supplier.get();
        } finally {
            scope.leaveScope();
        }
    }

    private <T> T scoped(Supplier<T> supplier) {
        scope.enterScope();
        try {
            return supplier.get();
        } finally {
            scope.leaveScope();
        }
    }

    private Value shuffleMessage(List<Value> message) {
        return valueShuffler.shuffle(message);
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
            public Value visit(LiteralValue value) {
                return value;
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
