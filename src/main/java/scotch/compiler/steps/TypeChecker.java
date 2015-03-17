package scotch.compiler.steps;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static scotch.compiler.error.SymbolNotFoundError.symbolNotFound;
import static scotch.compiler.syntax.definition.DefinitionEntry.entry;
import static scotch.compiler.syntax.reference.DefinitionReference.classRef;
import static scotch.compiler.syntax.reference.DefinitionReference.instanceRef;
import static scotch.compiler.syntax.value.Values.arg;
import static scotch.compiler.syntax.value.Values.entry;
import static scotch.compiler.syntax.value.Values.instance;
import static scotch.compiler.text.TextUtil.repeat;
import static scotch.compiler.util.Pair.pair;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import scotch.compiler.error.SyntaxError;
import scotch.symbol.Symbol;
import scotch.symbol.descriptor.DataConstructorDescriptor;
import scotch.symbol.descriptor.TypeInstanceDescriptor;
import scotch.symbol.type.InstanceType;
import scotch.symbol.type.SumType;
import scotch.symbol.type.Type;
import scotch.symbol.type.TypeScope;
import scotch.symbol.type.Types;
import scotch.symbol.type.Unification;
import scotch.symbol.type.VariableType;
import scotch.compiler.syntax.Scoped;
import scotch.compiler.syntax.definition.Definition;
import scotch.compiler.syntax.definition.DefinitionEntry;
import scotch.compiler.syntax.definition.DefinitionGraph;
import scotch.compiler.syntax.definition.ValueDefinition;
import scotch.compiler.syntax.definition.ValueSignature;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.reference.ValueReference;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.syntax.value.Argument;
import scotch.compiler.syntax.value.FunctionValue;
import scotch.compiler.syntax.value.InstanceMap;
import scotch.compiler.syntax.value.Method;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.text.SourceRange;

public class TypeChecker implements TypeScope {

    private static final Object mark = new Object();

    public static SyntaxError ambiguousTypeInstance(Symbol typeClass, List<Type> parameters, Set<TypeInstanceDescriptor> typeInstances, SourceRange location) {
        return new AmbiguousTypeInstanceError(typeClass, parameters, typeInstances, location);
    }

    public static SyntaxError typeInstanceNotFound(Symbol typeClass, List<Type> parameters, SourceRange location) {
        return new TypeInstanceNotFoundError(typeClass, parameters, location);
    }

    private final DefinitionGraph                           graph;
    private final Map<DefinitionReference, DefinitionEntry> entries;
    private final Deque<Scope>                              scopes;
    private final Deque<Scope>                              closures;
    private final Deque<Object>                             nestings;
    private final Deque<Map<Type, Argument>>                arguments;
    private final List<SyntaxError>                         errors;

    public TypeChecker(DefinitionGraph graph) {
        this.graph = graph;
        this.entries = new HashMap<>();
        this.scopes = new ArrayDeque<>();
        this.closures = new ArrayDeque<>();
        this.nestings = new ArrayDeque<>();
        this.arguments = new ArrayDeque<>(asList(ImmutableMap.of()));
        this.errors = new ArrayList<>();
    }

    public void addLocal(Symbol symbol) {
        closure().addLocal(symbol.getCanonicalName());
    }

    public Definition bind(ValueDefinition definition) {
        return bindMethods(definition
            .withBody(definition.getBody().bindTypes(this)));
    }

    @Override
    public Unification bind(VariableType variableType, Type targetType) {
        return scope().bind(variableType, targetType);
    }

    public void capture(Symbol symbol) {
        closure().capture(symbol.getCanonicalName());
    }

    public DefinitionGraph checkTypes() {
        map(graph.getSortedReferences(), Definition::checkTypes);
        return graph
            .copyWith(entries.values())
            .appendErrors(errors)
            .build();
    }

    public Optional<DataConstructorDescriptor> getDataConstructor(Symbol symbol) {
        return scope().getDataConstructor(symbol);
    }

    public <T extends Scoped> T enclose(T scoped, Supplier<T> supplier) {
        return scoped(scoped, () -> {
            enterNest();
            try {
                return supplier.get();
            } finally {
                leaveNest();
            }
        });
    }

    public void error(SyntaxError error) {
        errors.add(error);
    }

    @Override
    public void extendContext(Type type, Set<Symbol> additionalContext) {
        scope().extendContext(type, additionalContext);
    }

    public Optional<Value> findArgument(InstanceType type) {
        for (Map<Type, Argument> map : arguments) {
            if (map.containsKey(type)) {
                return Optional.of(map.get(type));
            }
        }
        return Optional.empty();
    }

    public Value findInstance(Method method, InstanceType instanceType) {
        Set<TypeInstanceDescriptor> typeInstances = scope().getTypeInstances(
            instanceType.getSymbol(),
            asList(instanceType.getBinding())
        );
        if (typeInstances.isEmpty()) {
            errors.add(typeInstanceNotFound(
                instanceType.getSymbol(),
                asList(instanceType.getBinding()),
                method.getSourceRange()
            ));
        } else if (typeInstances.size() > 1) {
            errors.add(ambiguousTypeInstance(
                instanceType.getSymbol(),
                asList(instanceType.getBinding()),
                typeInstances,
                method.getSourceRange()
            ));
        } else {
            return instance(method.getSourceRange(), instanceRef(typeInstances.iterator().next()), instanceType);
        }
        return method;
    }

    @Override
    public void generalize(Type type) {
        scope().generalize(type);
    }

    @Override
    public Type generate(Type type) {
        return scope().generate(type);
    }

    @Override
    public Set<Symbol> getContext(Type type) {
        return scope().getContext(type);
    }

    public Optional<Definition> getDefinition(DefinitionReference reference) {
        return graph.getDefinition(reference);
    }

    public Optional<Type> getRawValue(ValueReference valueRef) {
        return scope().getRawValue(valueRef);
    }

    @Override
    public Type getTarget(Type type) {
        return scope().getTarget(type);
    }

    public Type getType(ValueDefinition definition) {
        return scope()
            .getSignature(definition.getSymbol())
            .orElseGet(() -> scope()
                .getValue(definition.getSymbol())
                .orElseGet(() -> {
                    error(symbolNotFound(definition.getSymbol(), definition.getSourceRange()));
                    return reserveType();
                }));
    }

    @Override
    public void implement(Symbol typeClass, SumType type) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public boolean isBound(VariableType variableType) {
        return scope().isBound(variableType);
    }

    @Override
    public boolean isGeneric(VariableType variableType) {
        return scope().isGeneric(variableType);
    }

    @Override
    public boolean isImplemented(Symbol typeClass, SumType type) {
        return scope().isImplemented(typeClass, type);
    }

    public Definition keep(Definition definition) {
        return scoped(definition, () -> definition);
    }

    public List<DefinitionReference> map(List<DefinitionReference> references, BiFunction<? super Definition, TypeChecker, ? extends Definition> function) {
        return references.stream()
            .map(this::getDefinition)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(definition -> function.apply(definition, this))
            .map(Definition::getReference)
            .collect(toList());
    }

    public void redefine(ValueDefinition definition) {
        scope().redefineValue(definition.getSymbol(), definition.getType());
    }

    public void redefine(ValueSignature signature) {
        scope().redefineSignature(signature.getSymbol(), signature.getType());
    }

    public VariableType reserveType() {
        return scope().reserveType();
    }

    public Scope scope() {
        return scopes.peek();
    }

    public <T extends Scoped> T scoped(T scoped, Supplier<T> supplier) {
        enterScope(scoped);
        try {
            T result = supplier.get();
            entries.put(result.getReference(), entry(scope(), result.getDefinition()));
            return result;
        } finally {
            leaveScope();
        }
    }

    @Override
    public void specialize(Type type) {
        scope().specialize(type);
    }

    private Definition bindMethods(ValueDefinition definition) {
        InstanceMap instances = buildInstanceMap(definition);
        return scoped(definition, () -> {
            if (instances.isEmpty()) {
                return definition.withBody(definition.getBody().bindMethods(this));
            } else {
                List<Argument> instanceArguments = getAdditionalArguments(definition, instances);
                arguments.push(instanceArguments.stream()
                    .map(argument -> pair(argument.getType(), argument))
                    .reduce(new HashMap<>(), (map, pair) -> pair.into((type, argument) -> {
                        map.put(type, argument);
                        return map;
                    }), (left, right) -> {
                        left.putAll(right);
                        return left;
                    }));
                try {
                    return definition.withBody(definition.getBody().withArguments()
                        .map((reference, arguments) -> {
                            Scope scope = graph.getScope(reference);
                            List<String> locals = new ArrayList<>();
                            instanceArguments.forEach(argument -> {
                                scope.defineValue(argument.getSymbol(), argument.getType());
                                locals.add(argument.getName());
                            });
                            scope.prependLocals(locals);
                            return ImmutableList.<Argument>builder()
                                .addAll(instanceArguments)
                                .addAll(arguments)
                                .build();
                        })
                        .orElseGet(value -> {
                            Symbol functionSymbol = scope().reserveSymbol();
                            Scope functionScope = scope().enterScope();
                            scope().insertChild(functionScope);
                            FunctionValue function = FunctionValue.builder()
                                .withSourceRange(value.getSourceRange())
                                .withSymbol(functionSymbol)
                                .withArguments(instanceArguments)
                                .withBody(value)
                                .build();
                            entries.put(function.getReference(), entry(functionScope, function));
                            instanceArguments.forEach(argument -> {
                                functionScope.defineValue(argument.getSymbol(), argument.getType());
                                functionScope.addLocal(argument.getName());
                            });
                            return function;
                        })
                        .bindMethods(this));
                } finally {
                    arguments.pop();
                }
            }
        });
    }

    private InstanceMap buildInstanceMap(ValueDefinition definition) {
        InstanceMap.Builder builder = InstanceMap.builder();
        definition.getType().getInstanceMap().stream()
            .map(pair -> pair.into((type, className) -> pair(type, classRef(className))))
            .forEach(pair -> pair.into(builder::addInstance));
        return builder.build();
    }

    private Scope closure() {
        return closures.peek();
    }

    private void enterNest() {
        if (isNested()) {
            closures.push(scope());
        }
        nestings.push(mark);
    }

    private <T extends Scoped> void enterScope(T scoped) {
        Scope scope = graph.tryGetScope(scoped.getReference())
            .orElseGet(() -> Optional.ofNullable(entries.get(scoped.getReference()))
                .map(DefinitionEntry::getScope)
                .orElseThrow(() -> new IllegalArgumentException("No scope found for reference " + scoped.getReference())));
        scopes.push(scope);
    }

    private List<Argument> getAdditionalArguments(ValueDefinition definition, InstanceMap instances) {
        AtomicInteger counter = new AtomicInteger();
        return instances.stream()
            .flatMap(pair -> pair.into((type, classRefs) -> classRefs.stream()
                .map(classRef -> arg(
                    definition.getSourceRange().getStartRange(),
                    "#" + counter.getAndIncrement() + "i",
                    Types.instance(classRef.getSymbol(), type.simplify())
                ))))
            .collect(toList());
    }

    private boolean isNested() {
        return !nestings.isEmpty();
    }

    private void leaveNest() {
        nestings.pop();
        if (isNested()) {
            closures.pop();
        }
    }

    private void leaveScope() {
        scopes.pop();
    }

    public List<Value> bindMethods(List<Value> values) {
        return values.stream()
            .map(value -> value.bindMethods(this))
            .collect(toList());
    }

    @SuppressWarnings("unchecked")
    public <T extends Value> List<T> bindTypes(List<T> values) {
        return values.stream()
            .map(value -> (T) value.bindTypes(this))
            .collect(toList());
    }

    public List<Value> checkTypes(List<Value> values) {
        return values.stream()
            .map(value -> value.checkTypes(this))
            .collect(toList());
    }

    @EqualsAndHashCode(callSuper = false)
    @ToString
    public static class AmbiguousTypeInstanceError extends SyntaxError {

        private final Symbol                      typeClass;
        private final List<Type>                  parameters;
        private final Set<TypeInstanceDescriptor> typeInstances;
        private final SourceRange                 location;

        private AmbiguousTypeInstanceError(Symbol typeClass, List<Type> parameters, Set<TypeInstanceDescriptor> typeInstances, SourceRange location) {
            this.typeClass = typeClass;
            this.parameters = parameters;
            this.typeInstances = typeInstances;
            this.location = location;
        }

        @Override
        public String prettyPrint() {
            return prettyPrint_() + " " + location.prettyPrint();
        }

        @Override
        public String report(String indent, int indentLevel) {
            return location.report(indent, indentLevel) + "\n"
                + repeat(indent, indentLevel + 1) + prettyPrint_();
        }

        private String prettyPrint_() {
            return "Ambiguous instance of " + typeClass.quote()
                + " for parameters [" + parameters.stream().map(Type::toString).collect(joining(", ")) + "];"
                + " instances found in modules [" + typeInstances.stream().map(TypeInstanceDescriptor::getModuleName).collect(joining(", ")) + "]";
        }
    }

    @EqualsAndHashCode(callSuper = false)
    @ToString
    public static class TypeInstanceNotFoundError extends SyntaxError {

        private final Symbol      typeClass;
        private final List<Type>  parameters;
        private final SourceRange location;

        private TypeInstanceNotFoundError(Symbol typeClass, List<Type> parameters, SourceRange location) {
            this.typeClass = typeClass;
            this.parameters = ImmutableList.copyOf(parameters);
            this.location = location;
        }

        @Override
        public String prettyPrint() {
            return prettyPrint_() + " " + location.prettyPrint();
        }

        @Override
        public String report(String indent, int indentLevel) {
            return location.report(indent, indentLevel) + "\n"
                + repeat(indent, indentLevel + 1) + prettyPrint_();
        }

        private String prettyPrint_() {
            return "Instance of type class " + typeClass.quote() + " not found for parameters"
                + " [" + parameters.stream().map(Type::toString).collect(joining(", ")) + "]";
        }
    }
}
