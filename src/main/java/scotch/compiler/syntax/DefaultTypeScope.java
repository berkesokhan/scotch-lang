package scotch.compiler.syntax;

import static java.util.Collections.emptySet;
import static scotch.compiler.symbol.Type.fn;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.SymbolGenerator;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.Type.FunctionType;
import scotch.compiler.symbol.Type.SumType;
import scotch.compiler.symbol.Type.TypeVisitor;
import scotch.compiler.symbol.Type.VariableType;
import scotch.compiler.symbol.TypeScope;

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
    public void bind(VariableType variableType, Type targetType) {
        bind_(variableType.simplify(), targetType);
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
                    return genericMappings.computeIfAbsent(type, k -> symbolGenerator.reserveType().withContext(type.getContext()));
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
    public void specialize(Type type) {
        specializedTypes.add(type.simplify());
    }

    private void bind_(VariableType variableType, Type targetType) {
        if (isBound(variableType) && !getTarget(variableType).equals(targetType)) {
            throw new UnsupportedOperationException("Can't re-bind type " + variableType.prettyPrint() + " to new target "
                + targetType.prettyPrint() + "; current binding is incompatible: " + getTarget(variableType).prettyPrint());
        } else {
            bindings.put(variableType, targetType);
        }
    }
}
