package scotch.compiler.symbol.type;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static scotch.compiler.symbol.Symbol.fromString;
import static scotch.compiler.symbol.Unification.contextMismatch;
import static scotch.compiler.text.SourceRange.NULL_SOURCE;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import scotch.compiler.steps.NameQualifier;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.TypeScope;
import scotch.compiler.symbol.Unification;
import scotch.compiler.text.SourceRange;
import scotch.data.tuple.Tuple2;

public abstract class Type {

    public static FunctionType fn(Type argument, Type result) {
        return new FunctionType(NULL_SOURCE, argument, result);
    }

    public static InstanceType instance(Symbol symbol, Type binding) {
        return new InstanceType(symbol, binding);
    }

    public static InstanceType instance(String name, Type binding) {
        return instance(fromString(name), binding);
    }

    public static SumType sum(String name) {
        return sum(name, ImmutableList.of());
    }

    public static SumType sum(String name, Type... arguments) {
        return sum(name, asList(arguments));
    }

    public static SumType sum(String name, List<Type> arguments) {
        return sum(fromString(name), arguments);
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

    public static VariableSum varSum(String name, VariableType... parameters) {
        return varSum(name, asList(parameters));
    }

    private static VariableSum varSum(String name, List<VariableType> parameters) {
        return new VariableSum(name, parameters);
    }

    protected static int sort(Tuple2<VariableType, Symbol> left, Tuple2<VariableType, Symbol> right) {
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
            .map(item -> item instanceof String ? fromString((String) item) : (Symbol) item)
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

    Type() {
        // intentionally Optional.empty
    }

    public abstract Unification apply(SumType sum, TypeScope scope);

    @Override
    public abstract boolean equals(Object o);

    public Type generate(TypeScope scope) {
        return generate(scope, new HashSet<>());
    }

    public abstract Type genericCopy(TypeScope scope);

    public Set<Symbol> getContext() {
        return ImmutableSet.of();
    }

    public Set<Tuple2<VariableType, Symbol>> getContexts() {
        return gatherContext_();
    }

    public abstract Map<String, Type> getContexts(Type type, TypeScope scope);

    public List<Tuple2<VariableType, Symbol>> getInstanceMap() {
        return ImmutableList.of();
    }

    public abstract Class<?> getJavaType();

    public abstract String getSignature();

    public abstract SourceRange getSourceRange();

    public boolean hasContext() {
        return !getContexts().isEmpty();
    }

    @Override
    public abstract int hashCode();

    public abstract Type qualifyNames(NameQualifier qualifier);

    public abstract Unification rebind(TypeScope scope);

    public Type simplify() {
        return this;
    }

    @Override
    public abstract String toString();

    public abstract Unification unify(Type type, TypeScope scope);

    protected abstract boolean contains(VariableType type);

    protected String gatherContext() {
        Set<Tuple2<VariableType, Symbol>> context = gatherContext_();
        if (context.isEmpty()) {
            return "";
        } else {
            return "(" + context.stream()
                .map(tuple -> tuple.into((type, symbol) -> symbol.getSimpleName() + " " + type.getName()))
                .collect(joining(", ")) + ") => ";
        }
    }

    protected abstract Set<Tuple2<VariableType, Symbol>> gatherContext_();

    protected abstract Type generate(TypeScope scope, Set<Type> visited);

    protected abstract String getSignature_();

    protected abstract String toParenthesizedString();

    protected abstract String toString_();

    protected abstract Unification unifyWith(FunctionType target, TypeScope scope);

    protected abstract Unification unifyWith(VariableType target, TypeScope scope);

    protected abstract Unification unifyWith(SumType target, TypeScope scope);
}
