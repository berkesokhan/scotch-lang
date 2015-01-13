package scotch.compiler.parser;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.Definition.scopeDef;
import static scotch.compiler.syntax.DefinitionEntry.entry;
import static scotch.compiler.syntax.DefinitionReference.rootRef;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.syntax.Definition;
import scotch.compiler.syntax.Definition.DefinitionVisitor;
import scotch.compiler.syntax.Definition.ModuleDefinition;
import scotch.compiler.syntax.Definition.OperatorDefinition;
import scotch.compiler.syntax.Definition.RootDefinition;
import scotch.compiler.syntax.Definition.ValueDefinition;
import scotch.compiler.syntax.Definition.ValueSignature;
import scotch.compiler.syntax.DefinitionEntry;
import scotch.compiler.syntax.DefinitionGraph;
import scotch.compiler.syntax.DefinitionReference;
import scotch.compiler.syntax.DefinitionReference.DefinitionReferenceVisitor;
import scotch.compiler.syntax.PatternMatch;
import scotch.compiler.syntax.PatternMatch.CaptureMatch;
import scotch.compiler.syntax.PatternMatch.PatternMatchVisitor;
import scotch.compiler.syntax.PatternMatcher;
import scotch.compiler.syntax.Scope;
import scotch.compiler.syntax.Value;
import scotch.compiler.syntax.Value.Apply;
import scotch.compiler.syntax.Value.Argument;
import scotch.compiler.syntax.Value.BoolLiteral;
import scotch.compiler.syntax.Value.CharLiteral;
import scotch.compiler.syntax.Value.DoubleLiteral;
import scotch.compiler.syntax.Value.FunctionValue;
import scotch.compiler.syntax.Value.Identifier;
import scotch.compiler.syntax.Value.IntLiteral;
import scotch.compiler.syntax.Value.PatternMatchers;
import scotch.compiler.syntax.Value.StringLiteral;
import scotch.compiler.syntax.Value.ValueVisitor;

public class NameAccumulator implements
    DefinitionVisitor<Definition>,
    DefinitionReferenceVisitor<Optional<DefinitionReference>>,
    PatternMatchVisitor<PatternMatch>,
    ValueVisitor<Value> {

    private final DefinitionGraph                           graph;
    private final Map<DefinitionReference, DefinitionEntry> definitions;
    private final Deque<Scope>                              scopes;

    public NameAccumulator(DefinitionGraph graph) {
        this.graph = graph;
        this.definitions = new HashMap<>();
        this.scopes = new ArrayDeque<>();
    }

    public DefinitionGraph parse() {
        rootRef().accept(this);
        return graph.copyWith(definitions.values()).build();
    }

    @Override
    public Value visit(Apply apply) {
        return apply
            .withFunction(apply.getFunction().accept(this))
            .withArgument(apply.getArgument().accept(this));
    }

    @Override
    public Value visit(Argument argument) {
        defineValue(argument.getSymbol(), argument.getType());
        return argument;
    }

    @Override
    public Value visit(BoolLiteral literal) {
        return literal;
    }

    @Override
    public PatternMatch visit(CaptureMatch match) {
        currentScope().defineValue(match.getSymbol(), match.getType());
        currentScope().specialize(match.getType());
        return match;
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
            return function
                .withArguments(function.getArguments().stream()
                    .map(argument -> (Argument) argument.accept(this))
                    .collect(toList()))
                .withBody(function.getBody().accept(this));
        });
    }

    @Override
    public Value visit(Identifier identifier) {
        return identifier;
    }

    @Override
    public Value visit(IntLiteral literal) {
        return literal;
    }

    @Override
    public Definition visit(ModuleDefinition definition) {
        return definition.withDefinitions(definition.getDefinitions().stream()
            .map(reference -> reference.accept(this))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(toList()));
    }

    @Override
    public Definition visit(OperatorDefinition definition) {
        return definition;
    }

    @Override
    public Value visit(PatternMatchers matchers) {
        return matchers.withMatchers(matchers.getMatchers().stream()
            .map(this::visitMatcher)
            .collect(toList()));
    }

    @Override
    public Definition visit(RootDefinition definition) {
        return definition.withDefinitions(definition.getDefinitions().stream()
            .map(reference -> reference.accept(this))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(toList()));
    }

    @Override
    public Value visit(StringLiteral literal) {
        return literal;
    }

    @Override
    public Definition visit(ValueDefinition definition) {
        defineValue(definition.getSymbol(), definition.getType());
        currentScope().specialize(definition.getType());
        return definition.withBody(definition.getBody().accept(this));
    }

    @Override
    public Definition visit(ValueSignature signature) {
        defineSignature(signature);
        currentScope().specialize(signature.getType());
        return signature;
    }

    @Override
    public Optional<DefinitionReference> visitOtherwise(DefinitionReference reference) {
        return scoped(reference, () -> graph.getDefinition(reference)
            .map(definition -> definition.accept(this))
            .flatMap(this::collect));
    }

    @Override
    public PatternMatch visitOtherwise(PatternMatch match) {
        return match;
    }

    private Optional<DefinitionReference> collect(Definition definition) {
        definitions.put(definition.getReference(), entry(currentScope(), definition));
        return Optional.of(definition.getReference());
    }

    private Scope currentScope() {
        return scopes.peek();
    }

    private void defineSignature(ValueSignature signature) {
        currentScope().defineSignature(signature.getSymbol(), signature.getType());
    }

    private void defineValue(Symbol symbol, Type type) {
        currentScope().getParent().defineValue(symbol, type);
    }

    private <T> T scoped(DefinitionReference reference, Supplier<T> supplier) {
        scopes.push(graph.getScope(reference));
        try {
            return supplier.get();
        } finally {
            scopes.pop();
        }
    }

    private PatternMatcher visitMatcher(PatternMatcher matcher) {
        return scoped(matcher.getReference(), () -> {
            definitions.put(matcher.getReference(), entry(currentScope(), matcher));
            return matcher
                .withMatches(matcher.getMatches().stream()
                    .map(match -> match.accept(this))
                    .collect(toList()))
                .withBody(matcher.getBody().accept(this));
        });
    }
}
