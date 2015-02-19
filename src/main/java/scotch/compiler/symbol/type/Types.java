package scotch.compiler.symbol.type;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static scotch.compiler.symbol.Symbol.symbol;
import static scotch.compiler.symbol.type.Unification.contextMismatch;
import static scotch.compiler.text.SourceRange.NULL_SOURCE;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.google.common.collect.ImmutableList;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.TypeScope;
import scotch.compiler.util.Pair;

public class Types {

    public static ConstructorType ctor(Type head, Type tail) {
        return new ConstructorType(head, tail);
    }

    public static FunctionType fn(Type argument, Type result) {
        return new FunctionType(NULL_SOURCE, argument, result);
    }

    public static InstanceType instance(Symbol symbol, Type binding) {
        return new InstanceType(symbol, binding);
    }

    public static InstanceType instance(String name, Type binding) {
        return instance(symbol(name), binding);
    }

    public static SumType sum(String name) {
        return sum(name, ImmutableList.of());
    }

    public static SumType sum(String name, Type... arguments) {
        return sum(name, asList(arguments));
    }

    public static SumType sum(String name, List<Type> arguments) {
        return sum(symbol(name), arguments);
    }

    public static SumType sum(Symbol name, List<Type> arguments) {
        return new SumType(NULL_SOURCE, name, arguments);
    }

    public static SumType sum(Symbol symbol) {
        return sum(symbol, ImmutableList.of());
    }

    public static VariableType t(int id) {
        return t(id, emptyList());
    }

    public static VariableType t(int id, List context) {
        return var("t" + id, context);
    }

    public static VariableType var(String name) {
        return var(name, ImmutableList.of());
    }

    public static VariableType var(String name, Collection<?> context) {
        return new VariableType(NULL_SOURCE, name, toSymbols(context));
    }

    protected static int sort(Pair<VariableType, Symbol> left, Pair<VariableType, Symbol> right) {
        return left.into((t1, s1) -> right.into((t2, s2) -> {
            int result = t1.getName().compareTo(t2.getName());
            if (result != 0) {
                return result;
            }
            return s1.compareTo(s2);
        }));
    }

    @SuppressWarnings("unchecked")
    protected static Set<Symbol> toSymbols(Collection<?> context) {
        return context.stream()
            .map(item -> item instanceof String ? symbol((String) item) : (Symbol) item)
            .collect(Collectors.toSet());
    }

    protected static Unification unifyVariable(Type actual, VariableType target, TypeScope scope) {
        if (scope.isBound(target)) {
            return scope.getTarget(target).unify(actual, scope);
        } else if (target.getContext().isEmpty()) {
            return scope.bind(target, actual);
        } else if (scope.getContext(actual).containsAll(target.getContext())) {
            return scope.bind(target, actual);
        } else {
            return contextMismatch(target, actual, target.getContext(), scope.getContext(actual));
        }
    }
}
