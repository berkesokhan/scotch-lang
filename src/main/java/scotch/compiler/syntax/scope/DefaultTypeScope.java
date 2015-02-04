package scotch.compiler.syntax.scope;

import static scotch.compiler.symbol.Unification.failedBinding;
import static scotch.compiler.symbol.Unification.unified;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import com.google.common.collect.ImmutableSet;
import scotch.compiler.symbol.DataTypeDescriptor;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.SymbolGenerator;
import scotch.compiler.symbol.TypeScope;
import scotch.compiler.symbol.Unification;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.symbol.type.VariableType;

public class DefaultTypeScope implements TypeScope {

    private final SymbolGenerator        symbolGenerator;
    private final Map<Type, Type>        bindings;
    private final Map<Type, Set<Symbol>> contexts;
    private final Set<Type>              specializedTypes;

    public DefaultTypeScope(SymbolGenerator symbolGenerator) {
        this.symbolGenerator = symbolGenerator;
        this.bindings = new HashMap<>();
        this.contexts = new HashMap<>();
        this.specializedTypes = new HashSet<>();
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
    public DataTypeDescriptor getDataType(Symbol symbol) {
        throw new UnsupportedOperationException();
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
    public boolean isBound(VariableType variableType) {
        return bindings.containsKey(variableType.simplify());
    }

    @Override
    public boolean isGeneric(VariableType variableType) {
        return !specializedTypes.contains(variableType.simplify());
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
}
