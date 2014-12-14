package scotch.compiler.parser;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.DefinitionEntry.scopedEntry;
import static scotch.compiler.syntax.DefinitionReference.rootRef;
import static scotch.compiler.syntax.SyntaxError.ambiguousTypeInstance;
import static scotch.compiler.syntax.SyntaxError.typeInstanceNotFound;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import com.google.common.collect.ImmutableList;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.TypeClassDescriptor;
import scotch.compiler.symbol.TypeInstanceDescriptor;
import scotch.compiler.syntax.Definition;
import scotch.compiler.syntax.Definition.DefinitionVisitor;
import scotch.compiler.syntax.Definition.ModuleDefinition;
import scotch.compiler.syntax.Definition.RootDefinition;
import scotch.compiler.syntax.Definition.ValueDefinition;
import scotch.compiler.syntax.DefinitionEntry;
import scotch.compiler.syntax.DefinitionGraph;
import scotch.compiler.syntax.DefinitionReference;
import scotch.compiler.syntax.DefinitionReference.DefinitionReferenceVisitor;
import scotch.compiler.syntax.Scope;
import scotch.compiler.syntax.SyntaxError;
import scotch.compiler.syntax.Value;
import scotch.compiler.syntax.Value.Apply;
import scotch.compiler.syntax.Value.PatternMatchers;
import scotch.compiler.syntax.Value.UnboundMethod;
import scotch.compiler.syntax.Value.ValueVisitor;

public class MethodBinder implements
    DefinitionReferenceVisitor<DefinitionReference>,
    DefinitionVisitor<Definition>,
    ValueVisitor<Value> {

    private final DefinitionGraph                           graph;
    private final Map<DefinitionReference, DefinitionEntry> definitions;
    private final Deque<Scope>                              scopes;
    private final List<SyntaxError>                         errors;

    public MethodBinder(DefinitionGraph graph) {
        this.graph = graph;
        this.definitions = new HashMap<>();
        this.scopes = new ArrayDeque<>();
        this.errors = new ArrayList<>();
    }

    public DefinitionGraph bindMethods() {
        graph.getDefinition(rootRef()).map(definition -> definition.accept(this));
        return graph
            .copyWith(definitions.values())
            .withErrors(ImmutableList.<SyntaxError>builder()
                .addAll(graph.getErrors())
                .addAll(errors)
                .build())
            .build();
    }

    @Override
    public Value visit(Apply apply) {
        return apply
            .withFunction(apply.getFunction().accept(this))
            .withArgument(apply.getArgument().accept(this));
    }

    @Override
    public Value visit(UnboundMethod unboundMethod) {
        Scope scope = currentScope().enterScope();
        TypeClassDescriptor typeClass = scope.getMemberOf(unboundMethod.getValueRef());
        Type memberType = scope.getRawValue(unboundMethod.getValueRef().getSymbol());
        Map<String, Type> contexts = memberType.getContexts(unboundMethod.getType(), scope);
        List<Type> parameters = typeClass.renderParameters(contexts);
        if (typeClass.getParameters().equals(parameters)) {
            return unboundMethod;
        } else {
            Set<TypeInstanceDescriptor> typeInstances = scope.getTypeInstances(typeClass.getSymbol(), parameters);
            if (typeInstances.isEmpty()) {
                errors.add(typeInstanceNotFound(typeClass, parameters, unboundMethod.getSourceRange()));
                return unboundMethod;
            } else if (typeInstances.size() > 1) {
                errors.add(ambiguousTypeInstance(typeClass, parameters, typeInstances, unboundMethod.getSourceRange()));
                return unboundMethod;
            } else {
                return unboundMethod.bind(typeInstances.iterator().next());
            }
        }
    }

    @Override
    public Definition visit(ModuleDefinition definition) {
        return collect(definition.withDefinitions(mapDefinitions(definition.getDefinitions())));
    }

    @Override
    public Definition visit(RootDefinition definition) {
        return collect(definition.withDefinitions(mapDefinitions(definition.getDefinitions())));
    }

    @Override
    public Definition visit(ValueDefinition definition) {
        return collect(definition.withBody(definition.getBody().accept(this)));
    }

    @Override
    public Value visit(PatternMatchers matchers) {
        return matchers.withMatchers(matchers.getMatchers().stream()
                .map(matcher -> matcher.withBody(matcher.getBody().accept(this)))
                .collect(toList())
        );
    }

    @Override
    public Value visitOtherwise(Value value) {
        return value;
    }

    @Override
    public DefinitionReference visitOtherwise(DefinitionReference reference) {
        return scoped(reference, () -> graph.getDefinition(reference).get().accept(this).getReference());
    }

    private Definition collect(Definition definition) {
        definitions.put(definition.getReference(), scopedEntry(definition, getScope(definition.getReference())));
        return definition;
    }

    private Scope currentScope() {
        return scopes.peek();
    }

    private Scope getScope(DefinitionReference reference) {
        return graph.getScope(reference);
    }

    private List<DefinitionReference> mapDefinitions(List<DefinitionReference> definitions) {
        return definitions.stream()
            .map(reference -> reference.accept(this))
            .collect(toList());
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
