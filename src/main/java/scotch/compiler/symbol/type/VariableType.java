package scotch.compiler.symbol.type;

import static java.lang.Character.isLowerCase;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static scotch.compiler.symbol.type.Types.var;
import static scotch.compiler.symbol.type.Unification.circular;
import static scotch.compiler.symbol.type.Unification.mismatch;
import static scotch.compiler.util.Pair.pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import scotch.compiler.steps.NameQualifier;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.TypeScope;
import scotch.compiler.text.SourceRange;
import scotch.compiler.util.Pair;
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
    public HeadApplication applyWith(SumType type, TypeScope scope) {
        if (context.size() != 1) {
            throw new UnsupportedOperationException(); // TODO
        }
        Symbol typeClass = context.iterator().next();
        for (int i = 0; i <= type.getParameters().size(); i++) {
            List<Type> parameters = type.getParameters().subList(0, i);
            SumType head = type.withParameters(parameters);
            if (scope.isImplemented(typeClass, head)) {
                scope.bind(this, head);
                return HeadApplication.right(head, type.getParameters().subList(i, type.getParameters().size()));
            }
        }
        return HeadApplication.left(mismatch(this, type));
    }

    @Override
    public HeadZip applyZipWith(SumType type, TypeScope scope) {
        if (context.size() != 1) {
            throw new UnsupportedOperationException(); // TODO
        }
        Symbol typeClass = context.iterator().next();
        for (int i = 0; i <= type.getParameters().size(); i++) {
            List<Type> parameters = type.getParameters().subList(0, i);
            if (scope.isImplemented(typeClass, type.withParameters(parameters))) {
                return HeadZip.right(pair(this, type.withParameters(parameters)), type.getParameters().subList(i, type.getParameters().size()));
            }
        }
        return HeadZip.left();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof VariableType) {
            VariableType other = (VariableType) o;
            return Objects.equals(name, other.name)
                && Objects.equals(context, other.context);
        } else {
            return false;
        }
    }

    @Override
    public Type flatten() {
        return this;
    }

    public Set<Symbol> getContext() {
        return context;
    }

    @Override
    public Map<String, Type> getContexts(Type type, TypeScope scope) {
        Map<String, Type> map = new HashMap<>();
        if (!context.isEmpty() && context.containsAll(scope.getContext(type))) {
            map.put(name, type instanceof VariableType ? this : type);
        }
        return map;
    }

    @Override
    public List<Pair<VariableType, Symbol>> getInstanceMap() {
        List<Pair<VariableType, Symbol>> instances = new ArrayList<>();
        getContext().forEach(className -> instances.add(pair(this, className)));
        return instances;
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
            .map(symbol -> pair(symbol, qualifier.qualify(symbol)))
            .map(pair -> pair.into((symbol, result) -> result.orElseGet(() -> {
                qualifier.symbolNotFound(symbol, sourceRange);
                return symbol;
            })))
            .collect(toSet()));
    }

    @Override
    public VariableType simplify() {
        if (context.isEmpty()) {
            return this;
        } else {
            return var(name);
        }
    }

    public VariableType withContext(Collection<Symbol> context) {
        return new VariableType(sourceRange, name, context);
    }

    public VariableType withSourceRange(SourceRange sourceRange) {
        return new VariableType(sourceRange, name, context);
    }

    private Unification bind(Type target, TypeScope scope) {
        return scope.bind(this, target);
    }

    private Unification unifyWith_(Type target, TypeScope scope) {
        if (target.contains(this)) {
            return circular(target, this);
        } else if (scope.isBound(this)) {
            return target.unify(scope.getTarget(this), scope);
        } else if (target.contains(this) && !equals(target)) {
            return circular(target, this);
        } else {
            return bind(target, scope);
        }
    }

    @Override
    protected boolean contains(VariableType type) {
        return equals(type);
    }

    @Override
    protected List<Type> flatten_() {
        return ImmutableList.of(this);
    }

    @Override
    protected Set<Pair<VariableType, Symbol>> gatherContext_() {
        return ImmutableSortedSet.copyOf(Types::sort, context.stream().map(s -> pair(this, s)).collect(toList()));
    }

    @Override
    protected Type generate(TypeScope scope, Set<Type> visited) {
        if (visited.contains(this)) {
            return this;
        } else {
            visited.add(this);
            return scope.getTarget(this).generate(scope, visited);
        }
    }

    @Override
    protected Type genericCopy(TypeScope scope, Map<Type, Type> mappings) {
        if (scope.isGeneric(this)) {
            return mappings.computeIfAbsent(simplify(), k -> {
                VariableType variable = scope.reserveType()
                    .withContext(new HashSet<Symbol>() {{
                        addAll(getContext());
                        addAll(scope.getContext(VariableType.this));
                    }});
                scope.extendContext(variable, getContext());
                return variable;
            });
        } else {
            return this;
        }
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
    protected Unification unifyWith(ConstructorType target, TypeScope scope) {
        return unifyWith_(target, scope);
    }

    @Override
    protected Unification unifyWith(SumType target, TypeScope scope) {
        return unifyWith_(target, scope);
    }

    @Override
    protected Unification unifyWith(FunctionType target, TypeScope scope) {
        return unifyWith_(target, scope);
    }

    @Override
    protected Unification unifyWith(VariableType target, TypeScope scope) {
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

    @Override
    protected Unification unify_(Type type, TypeScope scope) {
        return type.unifyWith(this, scope);
    }

    @Override
    protected Optional<List<Pair<Type, Type>>> zip_(Type other, TypeScope scope) {
        return other.zipWith(this, scope);
    }
}
