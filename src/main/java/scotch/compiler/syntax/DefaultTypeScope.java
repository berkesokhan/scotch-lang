package scotch.compiler.syntax;

import static java.util.Collections.emptySet;
import static scotch.compiler.symbol.Type.fn;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.Type.FunctionType;
import scotch.compiler.symbol.Type.SumType;
import scotch.compiler.symbol.Type.TypeVisitor;
import scotch.compiler.symbol.Type.VariableType;
import scotch.compiler.symbol.TypeGenerator;
import scotch.compiler.symbol.TypeScope;

public class DefaultTypeScope implements TypeScope {

    private final TypeGenerator          typeGenerator;
    private final Map<Type, Type>        bindings;
    private final Map<Type, Set<Symbol>> contexts;
    private final Set<Type>              specializedTypes;
    private final Map<Type, Type>        genericMappings;

    public DefaultTypeScope(TypeGenerator typeGenerator) {
        this.typeGenerator = typeGenerator;
        this.bindings = new HashMap<>();
        this.contexts = new HashMap<>();
        this.specializedTypes = new HashSet<>();
        this.genericMappings = new HashMap<>();
    }

    @Override
    public void bind(VariableType variableType, Type targetType) {
        if (isBound(variableType)) {
            throw new UnsupportedOperationException("Can't re-bind type " + variableType + " to new target "
                + targetType + ": current binding is " + getTarget(variableType));
        } else {
            bindings.put(variableType, targetType);
        }
    }

    @Override
    public TypeScope enterScope() {
        return new DefaultTypeScope(typeGenerator);
    }

    @Override
    public void extendContext(Type type, Set<Symbol> additionalContext) {
        contexts.computeIfAbsent(type, k -> new LinkedHashSet<>()).addAll(additionalContext);
    }

    @Override
    public Type generate(Type type) {
        return type.accept(new TypeVisitor<Type>() {
            @Override
            public Type visit(FunctionType type) {
                return fn(generate(type.getArgument()), generate(type.getResult()));
            }

            @Override
            public Type visit(VariableType type) {
                return getTarget(type);
            }

            @Override
            public Type visit(SumType type) {
                return type;
            }

            @Override
            public Type visitOtherwise(Type type) {
                throw new UnsupportedOperationException("Can't generate " + type.getClass());
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
            public Type visit(VariableType type) {
                if (specializedTypes.contains(type.simplify())) {
                    return type;
                } else {
                    return genericMappings.computeIfAbsent(type, k -> typeGenerator.reserveType().withContext(type.getContext()));
                }
            }

            @Override
            public Type visit(SumType type) {
                return type;
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
    public Type getTarget(Type type) {
        return type.accept(new TypeVisitor<Type>() {
            @Override
            public Type visit(VariableType type) {
                Type result = type;
                while (bindings.containsKey(result)) {
                    result = bindings.get(result);
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
            public Type visitOtherwise(Type type) {
                throw new IllegalArgumentException("Can't get target of non-variable type " + type);
            }
        });
    }

    @Override
    public boolean isBound(VariableType variableType) {
        return bindings.containsKey(variableType);
    }

    @Override
    public void specialize(Type type) {
        specializedTypes.add(type.accept(new TypeVisitor<Type>() {
            @Override
            public Type visit(VariableType type) {
                return type.simplify();
            }

            @Override
            public Type visitOtherwise(Type type) {
                throw new IllegalArgumentException("Can't specialize type: " + type.prettyPrint());
            }
        }));
    }
}
