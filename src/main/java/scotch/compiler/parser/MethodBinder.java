package scotch.compiler.parser;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.symbol.Type.instance;
import static scotch.compiler.syntax.DefinitionEntry.scopedEntry;
import static scotch.compiler.syntax.DefinitionReference.classRef;
import static scotch.compiler.syntax.DefinitionReference.moduleRef;
import static scotch.compiler.syntax.DefinitionReference.rootRef;
import static scotch.compiler.syntax.SyntaxError.ambiguousTypeInstance;
import static scotch.compiler.syntax.SyntaxError.typeInstanceNotFound;
import static scotch.compiler.syntax.Value.apply;
import static scotch.compiler.syntax.Value.arg;
import static scotch.compiler.syntax.Value.fn;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import com.google.common.collect.ImmutableList;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.Type.FunctionType;
import scotch.compiler.symbol.Type.InstanceType;
import scotch.compiler.symbol.Type.TypeVisitor;
import scotch.compiler.symbol.Type.VariableType;
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
import scotch.compiler.syntax.InstanceMap;
import scotch.compiler.syntax.Scope;
import scotch.compiler.syntax.SyntaxError;
import scotch.compiler.syntax.Value;
import scotch.compiler.syntax.Value.Apply;
import scotch.compiler.syntax.Value.Argument;
import scotch.compiler.syntax.Value.FunctionValue;
import scotch.compiler.syntax.Value.Method;
import scotch.compiler.syntax.Value.PatternMatchers;
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
    public Value visit(FunctionValue function) {
        return function.withBody(function.getBody().accept(this));
    }

    @Override
    public Value visit(Method method) {
        if (method.getType().hasContext()) {
            return bindClass(method);
        } else {
            return bindInstances(method);
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
        InstanceMap instances = buildInstanceMap(definition);
        if (instances.isEmpty()) {
            return collect(definition.withBody(definition.getBody().accept(this)));
        } else {
            List<Argument> additionalArguments = getAdditionalArguments(definition, instances);
            return collect(definition
                .withRequiredInstances(instances)
                .withBody(definition.getBody().accept(new ValueVisitor<Value>() {
                    @Override
                    public Value visit(FunctionValue function) {
                        return function.withArguments(ImmutableList.<Argument>builder()
                            .addAll(additionalArguments)
                            .addAll(function.getArguments())
                            .build());
                    }

                    @Override
                    public Value visitOtherwise(Value value) {
                        return fn(value.getSourceRange(), currentScope().reserveSymbol(), additionalArguments, value);
                    }
                }).accept(this)));
        }
    }

    private List<Argument> getAdditionalArguments(ValueDefinition definition, InstanceMap instances) {
        AtomicInteger counter = new AtomicInteger();
        List<Argument> additionalArguments = instances.stream()
            .flatMap(tuple -> tuple.into((type, classRefs) -> classRefs.stream()
                .map(classRef -> arg(
                    definition.getSourceRange().getStartRange(),
                    "$i" + counter.getAndIncrement(),
                    instance(classRef.getSymbol(), type.simplify())
                ))))
            .collect(toList());
        currentScope().setInstanceArguments(additionalArguments);
        return additionalArguments;
    }

    private InstanceMap buildInstanceMap(ValueDefinition definition) {
        InstanceMap.Builder builder = InstanceMap.builder();
        definition.getType().accept(new TypeVisitor<Void>() {
            @Override
            public Void visit(FunctionType type) {
                type.getArgument().accept(this);
                type.getResult().accept(this);
                return null;
            }

            @Override
            public Void visit(VariableType type) {
                type.getContext().forEach(className -> builder.addInstance(type, classRef(className)));
                return null;
            }

            @Override
            public Void visitOtherwise(Type type) {
                return null;
            }
        });
        return builder.build();
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

    private Value bindClass(Method method) {
        List<InstanceType> types = method.getType().getContexts().stream()
            .map(tuple -> tuple.into((var, sym) -> instance(sym, var.simplify())))
            .collect(toList());
        List<Argument> arguments = currentScope().getInstanceArguments().stream()
            .filter(argument -> types.contains((InstanceType) argument.getType()))
            .collect(toList());
        if (arguments.size() == types.size()) {
            return arguments.stream()
                .map(arg -> (Value) arg)
                .reduce(method, (left, right) -> apply(left, right, left.getType().accept(new TypeVisitor<Type>() {
                    @Override
                    public Type visit(FunctionType type) {
                        return type.getResult();
                    }
                })));
        } else {
            throw new UnsupportedOperationException(); // TODO
        }
    }

    private Value bindInstances(Method method) {
        Scope scope = currentScope();
        TypeClassDescriptor typeClass = scope.getMemberOf(method.getValueRef());
        Type memberType = scope.getRawValue(method.getValueRef().getSymbol());
        Map<String, Type> contexts = memberType.getContexts(method.getTargetType(), scope);
        List<Type> parameters = typeClass.renderParameters(contexts);
        if (typeClass.getParameters().equals(parameters)) {
            return method;
        } else {
            Set<TypeInstanceDescriptor> typeInstances = scope.getTypeInstances(typeClass.getSymbol(), parameters);
            if (typeInstances.isEmpty()) {
                errors.add(typeInstanceNotFound(typeClass, parameters, method.getSourceRange()));
                return method;
            } else if (typeInstances.size() > 1) {
                errors.add(ambiguousTypeInstance(typeClass, parameters, typeInstances, method.getSourceRange()));
                return method;
            } else {
                TypeInstanceDescriptor descriptor = typeInstances.iterator().next();
                return method.bind(
                    classRef(descriptor.getTypeClass()),
                    moduleRef(descriptor.getModuleName()),
                    descriptor.getParameters(),
                    method.getInstances().stream()
                        .reduce(method.getType(), (left, right) -> left.accept(new TypeVisitor<Type>() {
                            @Override
                            public Type visit(FunctionType type) {
                                return type.getResult();
                            }
                        }))
                );
            }
        }
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
