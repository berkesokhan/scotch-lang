package scotch.compiler.symbol.type;

import static java.lang.Character.isLowerCase;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static scotch.compiler.symbol.Unification.circular;
import static scotch.data.tuple.TupleValues.tuple2;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import scotch.compiler.steps.NameQualifier;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.TypeScope;
import scotch.compiler.symbol.Unification;
import scotch.compiler.text.SourceRange;
import scotch.data.tuple.Tuple2;
import scotch.runtime.Callable;

public class VariableType extends Type {

    private final SourceRange sourceRange;
    private final String      name;
    private final Set<Symbol> context;

    VariableType(SourceRange sourceRange, String name, Collection<Symbol> context) {
        if (!isLowerCase(name.charAt(0))) {
            throw new IllegalArgumentException("Variable type should have lower-case name: got '" + name + "'");
        }
        this.sourceRange = sourceRange;
        this.name = name;
        this.context = ImmutableSet.copyOf(context);
    }

    @Override
    public <T> T accept(TypeVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public Unification apply(SumType sum, TypeScope scope) {
        return unify(sum, scope);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof scotch.compiler.symbol.type.VariableType) {
            scotch.compiler.symbol.type.VariableType other = (scotch.compiler.symbol.type.VariableType) o;
            return Objects.equals(name, other.name)
                && Objects.equals(context, other.context);
        } else {
            return false;
        }
    }

    public Set<Symbol> getContext() {
        return ImmutableSet.copyOf(context);
    }

    @Override
    public Map<String, Type> getContexts(Type type, TypeScope scope) {
        Map<String, Type> map = new HashMap<>();
        if (!context.isEmpty() && context.containsAll(scope.getContext(type))) {
            map.put(name, type.accept(new TypeVisitor<Type>() {
                @Override
                public Type visit(scotch.compiler.symbol.type.VariableType type) {
                    return scotch.compiler.symbol.type.VariableType.this;
                }

                @Override
                public Type visitOtherwise(Type type) {
                    return type;
                }
            }));
        }
        return map;
    }

    @Override
    public Class<?> getJavaType() {
        return Callable.class;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getSignature() {
        return sig(Object.class);
    }

    @Override
    public SourceRange getSourceRange() {
        return sourceRange;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, context);
    }

    public boolean is(String variable) {
        return Objects.equals(name, variable);
    }

    @Override
    public Type qualifyNames(NameQualifier qualifier) {
        return withContext(context.stream()
            .map(symbol -> tuple2(symbol, qualifier.qualify(symbol)))
            .map(tuple -> tuple.into((symbol, result) -> result.orElseGet(() -> {
                qualifier.symbolNotFound(symbol, sourceRange);
                return symbol;
            })))
            .collect(toSet()));
    }

    @Override
    public Type rebind(TypeScope scope) {
        return scope.reserveType();
    }

    @Override
    public scotch.compiler.symbol.type.VariableType simplify() {
        if (context.isEmpty()) {
            return this;
        } else {
            return var(name);
        }
    }

    @Override
    public String toString() {
        return gatherContext() + toString_();
    }

    @Override
    public Unification unify(Type type, TypeScope scope) {
        return type.unifyWith(this, scope);
    }

    public scotch.compiler.symbol.type.VariableType withContext(Collection<Symbol> context) {
        return new scotch.compiler.symbol.type.VariableType(sourceRange, name, context);
    }

    public scotch.compiler.symbol.type.VariableType withSourceRange(SourceRange sourceRange) {
        return new scotch.compiler.symbol.type.VariableType(sourceRange, name, context);
    }

    private Unification bind(Type target, TypeScope scope) {
        return scope.bind(this, target);
    }

    private Optional<Unification> unify_(Type target, TypeScope scope) {
        if (scope.isBound(this)) {
            return Optional.of(target.unify(scope.getTarget(this), scope));
        } else if (target.contains(this) && !equals(target)) {
            return Optional.of(circular(target, this));
        } else {
            return Optional.empty();
        }
    }

    @Override
    protected boolean contains(scotch.compiler.symbol.type.VariableType type) {
        return equals(type);
    }

    @Override
    protected Set<Tuple2<scotch.compiler.symbol.type.VariableType, Symbol>> gatherContext_() {
        return ImmutableSortedSet.copyOf(Type::sort, context.stream().map(s -> tuple2(this, s)).collect(toList()));
    }

    @Override
    protected String getSignature_() {
        return p(Object.class);
    }

    @Override
    protected String toParenthesizedString() {
        return toString_();
    }

    @Override
    protected String toString_() {
        return name;
    }

    @Override
    protected Unification unifyWith(SumType target, TypeScope scope) {
        return unify_(target, scope).orElseGet(() -> bind(target, scope));
    }

    @Override
    protected Unification unifyWith(FunctionType target, TypeScope scope) {
        if (target.contains(this)) {
            return circular(target, this);
        } else {
            return unify_(target, scope).orElseGet(() -> bind(target, scope));
        }
    }

    @Override
    protected Unification unifyWith(scotch.compiler.symbol.type.VariableType target, TypeScope scope) {
        if (scope.isBound(this)) {
            return target.unify(scope.getTarget(this), scope);
        } else {
            Set<Symbol> additionalContext = new HashSet<>();
            additionalContext.addAll(context);
            additionalContext.addAll(target.context);
            scope.extendContext(target, additionalContext);
            scope.extendContext(this, additionalContext);
            return bind(target, scope);
        }
    }
}
