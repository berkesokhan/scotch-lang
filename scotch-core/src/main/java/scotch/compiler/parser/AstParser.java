package scotch.compiler.parser;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.ast.DefinitionReference.rootRef;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import scotch.compiler.ast.Definition;
import scotch.compiler.ast.Definition.DefinitionVisitor;
import scotch.compiler.ast.Definition.ModuleDefinition;
import scotch.compiler.ast.Definition.OperatorDefinition;
import scotch.compiler.ast.Definition.RootDefinition;
import scotch.compiler.ast.Definition.UnshuffledPattern;
import scotch.compiler.ast.Definition.ValueDefinition;
import scotch.compiler.ast.DefinitionEntry;
import scotch.compiler.ast.DefinitionEntry.DefinitionEntryVisitor;
import scotch.compiler.ast.DefinitionReference;
import scotch.compiler.ast.DefinitionReference.DefinitionReferenceVisitor;
import scotch.compiler.ast.DefinitionReference.ModuleReference;
import scotch.compiler.ast.Import;
import scotch.compiler.ast.PatternMatch;
import scotch.compiler.ast.PatternMatch.CaptureMatch;
import scotch.compiler.ast.PatternMatch.EqualMatch;
import scotch.compiler.ast.PatternMatch.PatternMatchVisitor;
import scotch.compiler.ast.PatternMatcher;
import scotch.compiler.ast.SymbolNotFoundException;
import scotch.compiler.ast.SymbolResolver;
import scotch.compiler.ast.SymbolTable;
import scotch.compiler.ast.Type;
import scotch.compiler.ast.Type.TypeVisitor;
import scotch.compiler.ast.Value;
import scotch.compiler.ast.Value.Apply;
import scotch.compiler.ast.Value.Identifier;
import scotch.compiler.ast.Value.LiteralValue;
import scotch.compiler.ast.Value.Message;
import scotch.compiler.ast.Value.PatternMatchers;
import scotch.compiler.ast.Value.ValueVisitor;

public class AstParser implements
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

    public AstParser(SymbolTable symbols, SymbolResolver resolver) {
        this.symbols = symbols;
        this.scope = new ScopeBuilder(symbols.getSequence(), resolver);
        this.patternShuffler = new PatternShuffler(scope, this::visitMatcher);
        this.valueShuffler = new ValueShuffler(scope, value -> value.accept(this));
    }

    public SymbolTable analyze() {
        symbols.getDefinition(rootRef()).accept(this);
        return symbols.copyWith(scope.getSequence(), scope.getDefinitions());
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
            .orElseThrow(() -> new SymbolNotFoundException(identifier.getSymbol().toString()));
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
        return scoped(() -> Optional.of(collect(definition.withBody(definition.getBody().accept(this)))));
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
                    return message.getMembers().get(0);
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
            public Value visitOtherwise(Value value) {
                return value;
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
