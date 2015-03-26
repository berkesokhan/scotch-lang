package scotch.compiler.syntax.scope;

import static java.util.stream.Collectors.toList;
import static scotch.symbol.type.Unification.failedBinding;
import static scotch.symbol.type.Unification.unified;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import scotch.symbol.Symbol;
import scotch.symbol.util.SymbolGenerator;
import scotch.symbol.SymbolResolver;
import scotch.symbol.type.TypeScope;
import scotch.symbol.type.Unification;
import scotch.symbol.type.SumType;
import scotch.symbol.type.Type;
import scotch.symbol.type.VariableType;

public class DefaultTypeScope implements TypeScope {

    private final SymbolGenerator                   symbolGenerator;
    private final SymbolResolver                    resolver;
    private final Map<Type, Type>                   bindings;
    private final Map<Type, Set<Symbol>>            contexts;
    private final Set<Type>                         specializedTypes;
    private final Map<Symbol, List<Implementation>> implementations;

    public DefaultTypeScope(SymbolGenerator symbolGenerator, SymbolResolver resolver) {
        this.symbolGenerator = symbolGenerator;
        this.resolver = resolver;
        this.bindings = new HashMap<>();
        this.contexts = new HashMap<>();
        this.specializedTypes = new HashSet<>();
        this.implementations = new HashMap<>();
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
        return type.generate(this);
    }

    @Override
    public Set<Symbol> getContext(Type type) {
        if (type instanceof VariableType) {
            extendContext(type, type.getContext());
            return contexts.get(type);
        } else {
            return contexts.getOrDefault(type, ImmutableSet.of());
        }
    }

    @Override
    public Type getTarget(Type type) {
        Type result = type;
        while (bindings.containsKey(result.simplify())) {
            result = bindings.get(result.simplify());
        }
        if (result instanceof VariableType) {
            return ((VariableType) result).withContext(getContext(result));
        } else {
            return result;
        }
    }

    @Override
    public void implement(Symbol typeClass, SumType type) {
        implementations.computeIfAbsent(typeClass, k -> new ArrayList<>()).add(implement(type));
    }

    @Override
    public boolean isBound(VariableType variableType) {
        return bindings.containsKey(variableType.simplify());
    }

    @Override
    public boolean isGeneric(VariableType variableType) {
        return !specializedTypes.contains(variableType.simplify());
    }

    @Override
    public boolean isImplemented(Symbol typeClass, SumType type) {
        return isImplementedLocally(typeClass, type) || resolver.isImplemented(typeClass, type);
    }

    private Boolean isImplementedLocally(Symbol typeClass, SumType type) {
        return Optional.ofNullable(implementations.get(typeClass))
            .map(list -> list.stream().anyMatch(implementation -> implementation.isImplementedBy(type, this)))
            .orElse(false);
    }

    @Override
    public VariableType reserveType() {
        return symbolGenerator.reserveType();
    }

    @Override
    public void specialize(Type type) {
        specializedTypes.add(type.simplify());
    }

    private Unification bind_(VariableType variableType, Type targetType) {
        if (isBound(variableType) && !getTarget(variableType).simplify().equals(targetType)) {
            if (targetType instanceof VariableType) {
                if (isBound((VariableType) targetType)) {
                    return targetType.unify(variableType, this)
                        .map(unifiedType -> {
                            bindings.put(variableType, targetType);
                            return unified(unifiedType);
                        })
                        .orElseMap(unification -> failedBinding(targetType, variableType, getTarget(variableType)));
                } else {
                    bindings.put(targetType, getTarget(variableType));
                    return unified(variableType);
                }
            } else {
                return failedBinding(targetType, variableType, getTarget(variableType));
            }
        } else if (!getTarget(targetType).simplify().equals(variableType)) {
            bindings.put(variableType, targetType);
        }
        return unified(targetType);
    }

    private Implementation implement(SumType type) {
        List<Set<Symbol>> contexts = new ArrayList<>();
        type.getParameters().forEach(parameter -> contexts.add(parameter.getContext()));
        return new Implementation(type.getSymbol(), contexts);
    }

    private static final class Implementation {

        private final Symbol            symbol;
        private final List<Set<Symbol>> contexts;

        public Implementation(Symbol symbol, List<Set<Symbol>> contexts) {
            this.symbol = symbol;
            this.contexts = ImmutableList.copyOf(
                contexts.stream()
                    .map(ImmutableSet::copyOf)
                    .collect(toList())
            );
        }

        public boolean isImplementedBy(SumType type, TypeScope scope) {
            if (type.getSymbol().equals(symbol)) {
                List<Set<Symbol>> otherContexts = type.getParameters().stream()
                    .map(scope::getContext)
                    .collect(toList());
                if (otherContexts.size() == contexts.size()) {
                    for (int i = 0; i < contexts.size(); i++) {
                        if (!otherContexts.get(0).containsAll(contexts.get(0))) {
                            return false;
                        }
                    }
                    return true;
                }
            }
            return false;
        }
    }
}
