package scotch.compiler.parser;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static scotch.compiler.symbol.Symbol.unqualified;
import static scotch.compiler.symbol.Type.fn;
import static scotch.compiler.symbol.Type.instance;
import static scotch.compiler.syntax.DefinitionEntry.entry;
import static scotch.compiler.syntax.DefinitionReference.classRef;
import static scotch.compiler.syntax.DefinitionReference.instanceRef;
import static scotch.compiler.syntax.SyntaxError.ambiguousTypeInstance;
import static scotch.compiler.syntax.SyntaxError.typeError;
import static scotch.compiler.syntax.SyntaxError.typeInstanceNotFound;
import static scotch.compiler.syntax.Value.apply;
import static scotch.compiler.syntax.Value.arg;
import static scotch.compiler.syntax.Value.fn;
import static scotch.compiler.syntax.Value.instance;
import static scotch.data.tuple.TupleValues.tuple2;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import com.google.common.collect.ImmutableList;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.SymbolGenerator;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.Type.FunctionType;
import scotch.compiler.symbol.Type.InstanceType;
import scotch.compiler.symbol.Type.TypeVisitor;
import scotch.compiler.symbol.Type.VariableType;
import scotch.compiler.symbol.TypeInstanceDescriptor;
import scotch.compiler.symbol.Unification;
import scotch.compiler.symbol.Unification.UnificationVisitor;
import scotch.compiler.symbol.Unification.Unified;
import scotch.compiler.syntax.Definition;
import scotch.compiler.syntax.Definition.DefinitionVisitor;
import scotch.compiler.syntax.Definition.ModuleDefinition;
import scotch.compiler.syntax.Definition.RootDefinition;
import scotch.compiler.syntax.Definition.ScopeDefinition;
import scotch.compiler.syntax.Definition.ValueDefinition;
import scotch.compiler.syntax.Definition.ValueSignature;
import scotch.compiler.syntax.DefinitionEntry;
import scotch.compiler.syntax.DefinitionGraph;
import scotch.compiler.syntax.DefinitionReference;
import scotch.compiler.syntax.DefinitionReference.DefinitionReferenceVisitor;
import scotch.compiler.syntax.InstanceMap;
import scotch.compiler.syntax.PatternMatch;
import scotch.compiler.syntax.PatternMatch.CaptureMatch;
import scotch.compiler.syntax.PatternMatch.EqualMatch;
import scotch.compiler.syntax.PatternMatch.PatternMatchVisitor;
import scotch.compiler.syntax.PatternMatcher;
import scotch.compiler.syntax.Scope;
import scotch.compiler.syntax.SyntaxError;
import scotch.compiler.syntax.Value;
import scotch.compiler.syntax.Value.Apply;
import scotch.compiler.syntax.Value.Argument;
import scotch.compiler.syntax.Value.BoolLiteral;
import scotch.compiler.syntax.Value.BoundValue;
import scotch.compiler.syntax.Value.CharLiteral;
import scotch.compiler.syntax.Value.DoubleLiteral;
import scotch.compiler.syntax.Value.FunctionValue;
import scotch.compiler.syntax.Value.Identifier;
import scotch.compiler.syntax.Value.IntLiteral;
import scotch.compiler.syntax.Value.Method;
import scotch.compiler.syntax.Value.PatternMatchers;
import scotch.compiler.syntax.Value.StringLiteral;
import scotch.compiler.syntax.Value.UnboundMethod;
import scotch.compiler.syntax.Value.ValueVisitor;

public class TypeAnalyzer {

    private final DefinitionGraph                           graph;
    private final Map<DefinitionReference, DefinitionEntry> definitions;
    private final Deque<Scope>                              scopes;
    private final List<SyntaxError>                         errors;
    private       SymbolGenerator                           symbolGenerator;

    public TypeAnalyzer(DefinitionGraph graph) {
        this.graph = graph;
        this.definitions = new HashMap<>();
        this.scopes = new ArrayDeque<>();
        this.errors = new ArrayList<>();
        this.symbolGenerator = graph.getSymbolGenerator();
    }

    public DefinitionGraph analyze() {
        TypeChecker typeChecker = new TypeChecker();
        graph.getSortedReferences().forEach(reference -> reference.accept(typeChecker));
        return graph
            .copyWith(definitions.values())
            .withSequence(symbolGenerator)
            .appendErrors(errors)
            .build();
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

    private final class MethodBinder implements ValueVisitor<Value>, PatternMatchVisitor<PatternMatch> {

        private final ValueDefinition            definition;
        private final Deque<Map<Type, Argument>> arguments;

        private MethodBinder(ValueDefinition definition) {
            this.definition = definition;
            this.arguments = new ArrayDeque<>();
        }

        public ValueDefinition bind() {
            InstanceMap instances = buildInstanceMap(definition);
            return scoped(definition.getReference(), () -> {
                if (instances.isEmpty()) {
                    return definition.withBody(definition.getBody().accept(this));
                } else {
                    List<Argument> instanceArguments = getAdditionalArguments(definition, instances);
                    arguments.push(instanceArguments.stream()
                        .map(argument -> tuple2(argument.getType(), argument))
                        .reduce(new HashMap<>(), (map, tuple) -> tuple.into((type, argument) -> {
                            map.put(type, argument);
                            return map;
                        }), (left, right) -> {
                            left.putAll(right);
                            return left;
                        }));
                    try {
                        return definition.withBody(definition.getBody().accept(new ValueVisitor<Value>() {
                            @Override
                            public Value visit(FunctionValue function) {
                                instanceArguments.forEach(argument -> graph
                                    .getScope(function.getReference())
                                    .defineValue(argument.getSymbol(), argument.getType()));
                                return function.withArguments(ImmutableList.<Argument>builder()
                                    .addAll(instanceArguments)
                                    .addAll(function.getArguments())
                                    .build());
                            }

                            @Override
                            public Value visitOtherwise(Value value) {
                                Symbol functionSymbol = currentScope().reserveSymbol();
                                Scope functionScope = currentScope().enterScope();
                                currentScope().insert(functionScope);
                                FunctionValue function = fn(value.getSourceRange(), functionSymbol, instanceArguments, value);
                                definitions.put(function.getReference(), entry(functionScope, function));
                                instanceArguments.forEach(argument -> functionScope.defineValue(argument.getSymbol(), argument.getType()));
                                return function;
                            }
                        }).accept(this));
                    } finally {
                        arguments.pop();
                    }
                }
            });
        }

        @Override
        public Value visit(Apply apply) {
            return apply
                .withFunction(apply.getFunction().accept(this))
                .withArgument(apply.getArgument().accept(this));
        }

        @Override
        public Value visit(Argument argument) {
            return argument;
        }

        @Override
        public Value visit(BoolLiteral literal) {
            return literal;
        }

        @Override
        public Value visit(BoundValue boundValue) {
            return boundValue;
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
            return scoped(function.getReference(), () -> function.withBody(function.getBody().accept(this)));
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
        public Value visit(Method method) {
            List<InstanceType> instanceTypes = new ArrayList<>();
            Type type = method.getType();
            for (int i = 0; i < method.getInstanceCount(); i++) {
                instanceTypes.add((InstanceType) ((FunctionType) type).getArgument());
                type = ((FunctionType) type).getResult();
            }
            Value result = method;
            for (InstanceType instanceType : instanceTypes) {
                Value typeArgument;
                if (instanceType.isBound()) {
                    typeArgument = findInstance(method, instanceType);
                } else {
                    typeArgument = findArgument(instanceType);
                }
                result = apply(result, typeArgument, ((FunctionType) result.getType()).getResult());
            }
            return result;
        }

        @Override
        public Value visit(PatternMatchers matchers) {
            return matchers.withMatchers(matchers.getMatchers().stream()
                .map(this::visitMatcher)
                .collect(toList()));
        }

        @Override
        public Value visit(StringLiteral literal) {
            return literal;
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

        private Argument findArgument(Type type) {
            for (Map<Type, Argument> map : arguments) {
                if (map.containsKey(type)) {
                    return map.get(type);
                }
            }
            throw new UnsupportedOperationException();
        }

        private Value findInstance(Method method, InstanceType instanceType) {
            Value typeArgument;
            Set<TypeInstanceDescriptor> typeInstances = currentScope().getTypeInstances(
                instanceType.getSymbol(),
                asList(instanceType.getBinding())
            );
            if (typeInstances.isEmpty()) {
                errors.add(typeInstanceNotFound(
                    currentScope().getTypeClass(instanceType.getClassRef()),
                    asList(instanceType.getBinding()),
                    method.getSourceRange()
                ));
                typeArgument = null; // TODO
            } else if (typeInstances.size() > 1) {
                errors.add(ambiguousTypeInstance(
                    currentScope().getTypeClass(instanceType.getClassRef()),
                    asList(instanceType.getBinding()),
                    typeInstances,
                    method.getSourceRange()
                ));
                typeArgument = null; // TODO
            } else {
                typeArgument = instance(method.getSourceRange(), instanceRef(typeInstances.iterator().next()), instanceType);
            }
            return typeArgument;
        }

        private List<Argument> getAdditionalArguments(ValueDefinition definition, InstanceMap instances) {
            AtomicInteger counter = new AtomicInteger();
            return instances.stream()
                .flatMap(tuple -> tuple.into((type, classRefs) -> classRefs.stream()
                    .map(classRef -> arg(
                        definition.getSourceRange().getStartRange(),
                        "$i" + counter.getAndIncrement(),
                        instance(classRef.getSymbol(), type.simplify())
                    ))))
                .collect(toList());
        }

        private PatternMatcher visitMatcher(PatternMatcher matcher) {
            return matcher.withBody(matcher.getBody().accept(this));
        }
    }

    private final class TypeBinder implements ValueVisitor<Value>, PatternMatchVisitor<PatternMatch> {

        private final ValueDefinition definition;

        public TypeBinder(ValueDefinition definition) {
            this.definition = definition;
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
    }

    private final class TypeChecker implements
        DefinitionReferenceVisitor<DefinitionReference>,
        DefinitionVisitor<Definition>,
        ValueVisitor<Value>,
        PatternMatchVisitor<PatternMatch> {

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
                List<Type> argumentTypes = function.getArguments().stream()
                    .map(Argument::getType)
                    .collect(toList());
                argumentTypes.forEach(currentScope()::specialize);
                try {
                    return function
                        .withBody(function.getBody().accept(this))
                        .withArguments(function.getArguments().stream()
                            .map(arg -> arg.withType(currentScope().generate(arg.getType())))
                            .collect(toList()));
                } finally {
                    argumentTypes.forEach(currentScope()::generalize);
                }
            });
        }

        @Override
        public Value visit(IntLiteral literal) {
            return literal;
        }

        @Override
        public Value visit(StringLiteral literal) {
            return literal;
        }

        @Override
        public Definition visit(ModuleDefinition definition) {
            return collect(definition);
        }

        @Override
        public Definition visit(RootDefinition definition) {
            return collect(definition);
        }

        @Override
        public Definition visit(ScopeDefinition definition) {
            return collect(definition);
        }

        @Override
        public Definition visit(ValueDefinition definition) {
            Value body = definition.getBody().accept(this);
            Type type = currentScope().getSignature(definition.getSymbol()).orElseGet(() -> currentScope().getValue(definition.getSymbol()));
            return type.unify(body.getType(), currentScope()).accept(new UnificationVisitor<Definition>() {
                @Override
                public Definition visit(Unified unified) {
                    Type unifiedType = currentScope().generate(unified.getUnifiedType());
                    currentScope().redefineValue(definition.getSymbol(), unifiedType);
                    return collect(bind(definition.withBody(body).withType(unifiedType)));
                }

                @Override
                public Definition visitOtherwise(Unification unification) {
                    errors.add(typeError(unification, definition.getSourceRange()));
                    return collect(definition.withBody(body).withType(type));
                }
            });
        }

        @Override
        public Value visit(PatternMatchers matchers) {
            List<PatternMatcher> patterns = matchers.getMatchers().stream()
                .map(this::visitMatcher)
                .collect(toList());
            AtomicReference<Type> type = new AtomicReference<>(reserveType());
            patterns = patterns.stream()
                .map(pattern -> pattern.getType().unify(type.get(), currentScope()).accept(new UnificationVisitor<PatternMatcher>() {
                    @Override
                    public PatternMatcher visit(Unified unified) {
                        Type result = currentScope().generate(unified.getUnifiedType());
                        type.set(result);
                        return pattern.withType(result);
                    }

                    @Override
                    public PatternMatcher visitOtherwise(Unification unification) {
                        errors.add(typeError(unification.flip(), pattern.getSourceRange()));
                        return pattern;
                    }
                }))
                .collect(toList());
            return matchers.withMatchers(patterns).withType(type.get());
        }

        @Override
        public PatternMatch visit(CaptureMatch match) {
            Scope scope = currentScope();
            return scope.generate(match.getType())
                .unify(scope.getValue(unqualified(match.getArgument())), scope)
                .accept(new UnificationVisitor<PatternMatch>() {
                    @Override
                    public PatternMatch visit(Unified unified) {
                        return match.withType(unified.getUnifiedType());
                    }

                    @Override
                    public PatternMatch visitOtherwise(Unification unification) {
                        errors.add(typeError(unification, match.getSourceRange()));
                        return match;
                    }
                });
        }

        @Override
        public Value visit(Identifier identifier) {
            return identifier.bind(currentScope());
        }

        @Override
        public Value visit(Apply apply) {
            Value function = apply.getFunction().accept(this);
            Value argument = apply.getArgument().accept(this);
            Type resultType = reserveType();
            return function.getType().unify(fn(argument.getType(), resultType), currentScope()).accept(new UnificationVisitor<Value>() {
                @Override
                public Value visit(Unified unified) {
                    Value typedFunction = function.withType(currentScope().generate(function.getType()));
                    Value typedArgument = argument.withType(currentScope().generate(argument.getType()));
                    return apply
                        .withFunction(typedFunction)
                        .withArgument(typedArgument)
                        .withType(currentScope().generate(resultType));
                }

                @Override
                public Value visitOtherwise(Unification unification) {
                    errors.add(typeError(unification, apply.getSourceRange()));
                    return apply.withType(resultType);
                }
            });
        }

        @Override
        public PatternMatch visit(EqualMatch match) {
            return match.withValue(match.getValue().accept(this));
        }

        @Override
        public Definition visit(ValueSignature signature) {
            currentScope().redefineSignature(signature.getSymbol(), signature.getType());
            return signature;
        }

        @Override
        public DefinitionReference visitOtherwise(DefinitionReference reference) {
            return scoped(reference, () -> graph.getDefinition(reference).get().accept(this).getReference());
        }

        private ValueDefinition bind(ValueDefinition valueDefinition) {
            return new MethodBinder(new TypeBinder(valueDefinition).bind()).bind();
        }

        private Definition collect(Definition definition) {
            definitions.put(definition.getReference(), entry(getScope(definition.getReference()), definition));
            return definition;
        }

        private Scope currentScope() {
            return scopes.peek();
        }

        private Scope getScope(DefinitionReference reference) {
            return graph.getScope(reference);
        }

        private Type reserveType() {
            return symbolGenerator.reserveType();
        }

        private PatternMatcher visitMatcher(PatternMatcher matcher) {
            return scoped(matcher.getReference(), () -> {
                Value body = matcher.getBody().accept(this);
                List<PatternMatch> matches = matcher.getMatches().stream()
                    .map(match -> match.accept(this))
                    .map(match -> match.withType(currentScope().generate(match.getType())))
                    .collect(toList());
                return matcher
                    .withMatches(matches)
                    .withBody(body.withType(currentScope().generate(body.getType())));
            });
        }
    }
}
