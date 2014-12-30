package scotch.compiler.parser;

import static java.util.stream.Collectors.toList;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;
import scotch.compiler.symbol.Type;
import scotch.compiler.syntax.Definition.ValueDefinition;
import scotch.compiler.syntax.DefinitionGraph;
import scotch.compiler.syntax.DefinitionReference;
import scotch.compiler.syntax.PatternMatch;
import scotch.compiler.syntax.PatternMatch.CaptureMatch;
import scotch.compiler.syntax.PatternMatch.EqualMatch;
import scotch.compiler.syntax.PatternMatch.PatternMatchVisitor;
import scotch.compiler.syntax.Scope;
import scotch.compiler.syntax.Value;
import scotch.compiler.syntax.Value.Apply;
import scotch.compiler.syntax.Value.Argument;
import scotch.compiler.syntax.Value.FunctionValue;
import scotch.compiler.syntax.Value.PatternMatchers;
import scotch.compiler.syntax.Value.UnboundMethod;
import scotch.compiler.syntax.Value.ValueVisitor;

public class TypeBinder implements ValueVisitor<Value>, PatternMatchVisitor<PatternMatch> {

    private final DefinitionGraph graph;
    private final ValueDefinition definition;
    private final Deque<Scope>    scopes;

    public TypeBinder(DefinitionGraph graph, ValueDefinition definition) {
        this.graph = graph;
        this.definition = definition;
        this.scopes = new ArrayDeque<>();
    }

    public ValueDefinition bind() {
        return scoped(
            definition.getReference(),
            () -> definition
                .withType(generate(definition.getType()))
                .withBody(definition.getBody().accept(this))
        );
    }

    @Override
    public Value visit(Apply apply) {
        return apply
            .withFunction(apply.getFunction().accept(this))
            .withArgument(apply.getArgument().accept(this))
            .withType(generate(apply.getType()));
    }

    @Override
    public PatternMatch visit(EqualMatch match) {
        return match.withValue(match.getValue().accept(this));
    }

    @Override
    public PatternMatch visit(CaptureMatch match) {
        return match.withType(generate(match.getType()));
    }

    @Override
    public Value visit(FunctionValue function) {
        return scoped(function.getReference(), () -> function
            .withArguments(function.getArguments().stream()
                .map(argument -> (Argument) argument.accept(this))
                .collect(toList()))
            .withBody(function.getBody().accept(this)));
    }

    @Override
    public Value visit(PatternMatchers matchers) {
        return matchers
            .withMatchers(matchers.getMatchers().stream()
                .map(matcher -> matcher
                    .withMatches(matcher.getMatches().stream().map(match -> match.accept(this)).collect(toList()))
                    .withBody(matcher.getBody().accept(this))
                    .withType(generate(matcher.getType())))
                .collect(toList()))
            .withType(generate(matchers.getType()));
    }

    @Override
    public Value visit(UnboundMethod unboundMethod) {
        return unboundMethod.bind(currentScope()).accept(this);
    }

    @Override
    public Value visitOtherwise(Value value) {
        return value.withType(generate(value.getType()));
    }

    private Scope currentScope() {
        return scopes.peek();
    }

    private Type generate(Type type) {
        return currentScope().generate(type);
    }

    private <T> T scoped(DefinitionReference reference, Supplier<T> supplier) {
        scopes.push(graph.getScope(reference));
        try {
            return supplier.get();
        } finally {
            scopes.pop();
        }
    }
}
