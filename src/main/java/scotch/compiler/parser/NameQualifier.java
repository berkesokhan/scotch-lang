package scotch.compiler.parser;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static scotch.compiler.syntax.Definition.scopeDef;
import static scotch.compiler.syntax.DefinitionEntry.entry;
import static scotch.compiler.syntax.DefinitionReference.rootRef;
import static scotch.compiler.syntax.SyntaxError.symbolNotFound;
import static scotch.data.tuple.TupleValues.tuple2;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.Type.FunctionType;
import scotch.compiler.symbol.Type.SumType;
import scotch.compiler.symbol.Type.TypeVisitor;
import scotch.compiler.symbol.Type.VariableType;
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
import scotch.compiler.syntax.PatternMatch.PatternMatchVisitor;
import scotch.compiler.syntax.PatternMatcher;
import scotch.compiler.syntax.Scope;
import scotch.compiler.syntax.SyntaxError;
import scotch.compiler.syntax.Value;
import scotch.compiler.syntax.Value.Apply;
import scotch.compiler.syntax.Value.BoolLiteral;
import scotch.compiler.syntax.Value.CharLiteral;
import scotch.compiler.syntax.Value.DoubleLiteral;
import scotch.compiler.syntax.Value.FunctionValue;
import scotch.compiler.syntax.Value.Identifier;
import scotch.compiler.syntax.Value.IntLiteral;
import scotch.compiler.syntax.Value.PatternMatchers;
import scotch.compiler.syntax.Value.StringLiteral;
import scotch.compiler.syntax.Value.ValueVisitor;

public class NameQualifier implements
    DefinitionVisitor<Definition>,
    DefinitionReferenceVisitor<DefinitionReference>,
    PatternMatchVisitor<PatternMatch>,
    ValueVisitor<Value>,
    TypeVisitor<Type> {

    private final DefinitionGraph                           graph;
    private final Map<DefinitionReference, DefinitionEntry> definitions;
    private final Deque<Scope>                              scopes;
    private final List<SyntaxError>                         errors;

    public NameQualifier(DefinitionGraph graph) {
        this.graph = graph;
        this.definitions = new HashMap<>();
        this.scopes = new ArrayDeque<>();
        this.errors = new ArrayList<>();
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
        return currentScope().qualify(identifier.getSymbol())
            .map(identifier::withSymbol)
            .orElseGet(() -> {
                errors.add(symbolNotFound(identifier.getSymbol(), identifier.getSourceRange()));
                return identifier;
            });
    }

    @Override
    public Value visit(IntLiteral literal) {
        return literal;
    }

    @Override
    public Definition visit(ModuleDefinition definition) {
        return definition.withDefinitions(definition.getDefinitions().stream()
            .map(reference -> reference.accept(this))
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
            .collect(toList()));
    }

    @Override
    public Value visit(StringLiteral literal) {
        return literal;
    }

    @Override
    public Type visit(SumType type) {
        return type.withSymbol(currentScope().qualify(type.getSymbol()).orElseGet(() -> {
            errors.add(symbolNotFound(type.getSymbol(), type.getSourceRange()));
            return type.getSymbol();
        }));
    }

    @Override
    public Type visit(FunctionType type) {
        return type
            .withArgument(type.getArgument().accept(this))
            .withResult(type.getResult().accept(this));
    }

    @Override
    public Type visit(VariableType type) {
        return type.withContext(type.getContext().stream()
            .map(symbol -> tuple2(symbol, currentScope().qualify(symbol)))
            .map(tuple -> tuple.into((symbol, result) -> result.orElseGet(() -> {
                errors.add(symbolNotFound(symbol, type.getSourceRange()));
                return symbol;
            })))
            .collect(toSet()));
    }

    @Override
    public Definition visit(ValueDefinition definition) {
        return scoped(definition.getReference(), () -> definition.withBody(definition.getBody().accept(this)));
    }

    @Override
    public Definition visit(ValueSignature signature) {
        return signature.withType(signature.getType().accept(this));
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
            definitions.put(matcher.getReference(), entry(currentScope(), matcher));
            return matcher.withBody(matcher.getBody().accept(this));
        });
    }
}
