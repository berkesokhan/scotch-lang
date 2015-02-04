package scotch.compiler.symbol.type;

import static java.util.stream.Collectors.joining;
import static scotch.compiler.symbol.Unification.mismatch;
import static scotch.compiler.util.Pair.pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import scotch.compiler.steps.NameQualifier;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.TypeScope;
import scotch.compiler.symbol.Unification;
import scotch.compiler.text.SourceRange;
import scotch.compiler.util.Pair;

public abstract class Type {

    Type() {
        // intentionally empty
    }

    public abstract Unification apply(SumType sum, TypeScope scope);

    @Override
    public abstract boolean equals(Object o);

    public Type generate(TypeScope scope) {
        return generate(scope, new HashSet<>());
    }

    public final Type genericCopy(TypeScope scope) {
        return genericCopy(scope, new HashMap<>());
    }

    public Set<Symbol> getContext() {
        return ImmutableSet.of();
    }

    public Set<Pair<VariableType, Symbol>> getContexts() {
        return gatherContext_();
    }

    public abstract Map<String, Type> getContexts(Type type, TypeScope scope);

    public List<Pair<VariableType, Symbol>> getInstanceMap() {
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

    public Unification unify(Type type, TypeScope scope) {
        return generate(scope).unify_(type.generate(scope), scope);
    }

    public Optional<Map<Type, Type>> zip(Type other, TypeScope scope) {
        return generate(scope).zip_(other.generate(scope))
            .map(list -> {
                Map<Type, Type> map = new HashMap<>();
                list.stream()
                    .map(pair -> pair.into((key, value) -> pair(key.simplify(), value)))
                    .forEach(pair -> pair.into(map::put));
                return map;
            });
    }

    protected abstract Optional<List<Pair<Type, Type>>> zip_(Type other);

    protected Optional<List<Pair<Type, Type>>> zipWith(FunctionType target) {
        return Optional.empty();
    }

    protected Optional<List<Pair<Type, Type>>> zipWith(SumType target) {
        return Optional.empty();
    }

    protected Optional<List<Pair<Type, Type>>> zipWith(VariableType target) {
        return Optional.of(ImmutableList.of(pair(target, this)));
    }

    protected abstract boolean contains(VariableType type);

    protected String gatherContext() {
        Set<Pair<VariableType, Symbol>> context = gatherContext_();
        if (context.isEmpty()) {
            return "";
        } else {
            return "(" + context.stream()
                .map(pair -> pair.into((type, symbol) -> symbol.getSimpleName() + " " + type.getName()))
                .collect(joining(", ")) + ") => ";
        }
    }

    protected abstract Set<Pair<VariableType, Symbol>> gatherContext_();

    protected abstract Type generate(TypeScope scope, Set<Type> visited);

    protected abstract Type genericCopy(TypeScope scope, Map<Type, Type> mappings);

    protected abstract String getSignature_();

    protected abstract String toParenthesizedString();

    protected abstract String toString_();

    protected abstract Unification unifyWith(FunctionType target, TypeScope scope);

    protected abstract Unification unifyWith(VariableType target, TypeScope scope);

    protected abstract Unification unifyWith(SumType target, TypeScope scope);

    protected Unification unifyWith(VariableSum target, TypeScope scope) {
        return mismatch(target, this);
    }

    protected abstract Unification unify_(Type type, TypeScope scope);
}
