package scotch.compiler.syntax.scope;

import static java.util.Collections.emptySet;
import static scotch.compiler.symbol.type.Type.fn;
import static scotch.compiler.symbol.Unification.failedBinding;
import static scotch.compiler.symbol.Unification.unified;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import scotch.compiler.symbol.DataTypeDescriptor;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.SymbolGenerator;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.symbol.type.FunctionType;
import scotch.compiler.symbol.type.InstanceType;
import scotch.compiler.symbol.type.SumType;
import scotch.compiler.symbol.type.Type.TypeVisitor;
import scotch.compiler.symbol.type.VariableType;
import scotch.compiler.symbol.TypeScope;
import scotch.compiler.symbol.Unification;
import scotch.compiler.symbol.Unification.UnificationVisitor;
import scotch.compiler.symbol.Unification.Unified;

public class DefaultTypeScope implements TypeScope {

    private final SymbolGenerator        symbolGenerator;
    private final Map<Type, Type>        bindings;
    private final Map<Type, Set<Symbol>> contexts;
    private final Set<Type>              specializedTypes;
    private final Map<Type, Type>        genericMappings;

    public DefaultTypeScope(SymbolGenerator symbolGenerator) {
        this.symbolGenerator = symbolGenerator;
        this.bindings = new HashMap<>();
        this.contexts = new HashMap<>();
        this.specializedTypes = new HashSet<>();
        this.genericMappings = new HashMap<>();
    }

    @Override
    public Unification bind(VariableType variableType, Type targetType) {
        return bind_(variableType.simplify(), targetType);
    }

    @Override
    public void extendContext(Type type, Set<Symbol> additionalContext) {
        contexts.computeIfAbsent(type, k -> new LinkedHashSet<>()).addAll(additionalContext);
    }

    @Override
    public void generalize(Type type) {
        specializedTypes.remove(type.simplify());
    }

    @Override
    public Type generate(Type type) {
        Set<Type> visited = new HashSet<>();
        return type.accept(new TypeVisitor<Type>() {
            @Override
            public Type visit(FunctionType type) {
                return fn(generate(type.getArgument()), generate(type.getResult()));
            }

            @Override
            public Type visit(InstanceType type) {
                return type.withBinding(generate(type.getBinding()));
            }

            @Override
            public Type visit(SumType type) {
                return type;
            }

            @Override
            public Type visit(VariableType type) {
                if (visited.contains(type)) {
                    return type;
                } else {
                    visited.add(type);
                    return getTarget(type).accept(this);
                }
            }
        });
    }

    @Override
    public Type genericCopy(Type type) {
        return type.accept(new TypeVisitor<Type>() {
            @Override
            public Type visit(FunctionType type) {
                return type
                    .withArgument(genericCopy(type.getArgument()))
                    .withResult(genericCopy(type.getResult()));
            }

            @Override
            public Type visit(InstanceType type) {
                return type;
            }

            @Override
            public Type visit(SumType type) {
                return type;
            }

            @Override
            public Type visit(VariableType type) {
                if (specializedTypes.contains(type.simplify())) {
                    return type;
                } else {
                    return genericMappings.computeIfAbsent(type, k -> symbolGenerator.reserveType().withContext(type.getContext()));
                }
            }
        });
    }

    @Override
    public Set<Symbol> getContext(Type type) {
        return type.accept(new TypeVisitor<Set<Symbol>>() {
            @Override
            public Set<Symbol> visit(VariableType type) {
                extendContext(type, type.getContext());
                return contexts.get(type);
            }

            @Override
            public Set<Symbol> visitOtherwise(Type type) {
                return contexts.getOrDefault(type, emptySet());
            }
        });
    }

    @Override
    public DataTypeDescriptor getDataType(Symbol symbol) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Type getTarget(Type type) {
        Type result = type;
        while (bindings.containsKey(result.simplify())) {
            result = bindings.get(result.simplify());
        }
        return result.accept(new TypeVisitor<Type>() {
            @Override
            public Type visit(VariableType type) {
                return type.withContext(getContext(type));
            }

            @Override
            public Type visitOtherwise(Type type) {
                return type;
            }
        });
    }

    @Override
    public boolean isBound(VariableType variableType) {
        return bindings.containsKey(variableType);
    }

    @Override
    public Type reserveType() {
        return symbolGenerator.reserveType();
    }

    @Override
    public void specialize(Type type) {
        specializedTypes.add(type.simplify());
    }

    private Unification bind_(VariableType variableType, Type targetType) {
        if (isBound(variableType) && !getTarget(variableType).simplify().equals(targetType)) {
            return targetType.accept(new TypeVisitor<Unification>() {
                @Override
                public Unification visit(VariableType type) {
                    if (isBound(type)) {
                        return type.unify(variableType, DefaultTypeScope.this).accept(new UnificationVisitor<Unification>() {
                            @Override
                            public Unification visit(Unified unified) {
                                bindings.put(variableType, targetType);
                                return unified;
                            }

                            @Override
                            public Unification visitOtherwise(Unification unification) {
                                return failedBinding(targetType, variableType, getTarget(variableType));
                            }
                        });
                    } else {
                        bindings.put(targetType, getTarget(variableType));
                        return unified(variableType);
                    }
                }

                @Override
                public Unification visitOtherwise(Type type) {
                    return failedBinding(targetType, variableType, getTarget(variableType));
                }
            });
        } else if (!getTarget(targetType).simplify().equals(variableType)) {
            bindings.put(variableType, targetType);
        }
        return unified(targetType);
    }
}
