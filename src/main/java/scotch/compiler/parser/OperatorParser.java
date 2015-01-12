package scotch.compiler.parser;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.Definition.scopeDef;
import static scotch.compiler.syntax.DefinitionEntry.entry;
import static scotch.compiler.syntax.DefinitionReference.rootRef;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
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
import scotch.compiler.syntax.DefinitionGraph;
import scotch.compiler.syntax.DefinitionReference;
import scotch.compiler.syntax.DefinitionReference.DefinitionReferenceVisitor;
import scotch.compiler.syntax.PatternMatcher;
import scotch.compiler.syntax.Scope;
import scotch.compiler.syntax.Value;
import scotch.compiler.syntax.Value.Apply;
import scotch.compiler.syntax.Value.BoolLiteral;
import scotch.compiler.syntax.Value.CharLiteral;
import scotch.compiler.syntax.Value.DoubleLiteral;
import scotch.compiler.syntax.Value.FunctionValue;
import scotch.compiler.syntax.Value.Identifier;
import scotch.compiler.syntax.Value.IntLiteral;
import scotch.compiler.syntax.Value.Let;
import scotch.compiler.syntax.Value.Message;
import scotch.compiler.syntax.Value.PatternMatchers;
import scotch.compiler.syntax.Value.StringLiteral;
import scotch.compiler.syntax.Value.ValueVisitor;

public class OperatorParser implements
    ValueVisitor<Value>,
    DefinitionVisitor<Definition>,
    DefinitionReferenceVisitor<DefinitionReference> {

    private final DefinitionGraph                           graph;
    private final Map<DefinitionReference, DefinitionEntry> definitions;
    private final Deque<Scope>                              scopes;

    public OperatorParser(DefinitionGraph graph) {
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
        return identifier;
    }

    @Override
    public Value visit(IntLiteral literal) {
        return literal;
    }

    @Override
    public Value visit(Let let) {
        return scoped(let.getReference(), () -> {
            collect(scopeDef(let));
            return let
                .withDefinitions(let.getDefinitions().stream()
                    .map(reference -> reference.accept(this))
                    .collect(toList()))
                .withBody(let.getBody().accept(this));
        });
    }

    @Override
    public Definition visit(ModuleDefinition definition) {
        return definition.withDefinitions(definition.getDefinitions().stream()
            .map(reference -> reference.accept(this))
            .collect(toList()));
    }

    @Override
    public Value visit(Message message) {
        return message.withMembers(message.getMembers().stream()
            .map(member -> member.accept(this))
            .collect(toList()));
    }

    @Override
    public Definition visit(OperatorDefinition definition) {
        currentScope().defineOperator(definition.getSymbol(), definition.getOperator());
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
            .collect(toList()));
    }

    @Override
    public Value visit(StringLiteral literal) {
        return literal;
    }

    @Override
    public Definition visit(UnshuffledPattern pattern) {
        return collect(pattern);
    }

    @Override
    public Definition visit(ValueDefinition definition) {
        return definition.withBody(definition.getBody().accept(this));
    }

    @Override
    public Definition visit(ValueSignature signature) {
        return collect(signature);
    }

    @Override
    public DefinitionReference visitOtherwise(DefinitionReference reference) {
        return scoped(reference, () -> graph.getDefinition(reference)
            .map(definition -> definition.accept(this))
            .map(this::collect)
            .map(Definition::getReference)
            .get());
    }

    private Definition collect(Definition definition) {
        definitions.put(definition.getReference(), entry(currentScope(), definition));
        return definition;
    }

    private Scope currentScope() {
        return scopes.peek();
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
            definitions.put(
                matcher.getReference(),
                entry(currentScope(), scopeDef(matcher.getSourceRange(), matcher.getSymbol()))
            );
            return matcher.withBody(matcher.getBody().accept(this));
        });
    }
}
