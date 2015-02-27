package scotch.compiler.symbol.type;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import scotch.compiler.symbol.NameQualifier;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.text.SourceRange;
import scotch.compiler.util.Pair;

public class InstanceType extends Type {

    private final Symbol symbol;
    private final Type   binding;

    InstanceType(Symbol symbol, Type binding) {
        this.symbol = symbol;
        this.binding = binding;
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof InstanceType && Objects.equals(symbol, ((InstanceType) o).symbol);
    }

    @Override
    public Type flatten() {
        return withBinding(binding.flatten());
    }

    @Override
    public Type generate(TypeScope scope, Set<Type> visited) {
        return withBinding(binding.generate(scope, visited));
    }

    public Type getBinding() {
        return binding;
    }

    @Override
    public Map<String, Type> getContexts(Type type, TypeScope scope) {
        return ImmutableMap.of();
    }

    @Override
    public Class<?> getJavaType() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public String getSignature() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public SourceRange getSourceRange() {
        throw new UnsupportedOperationException(); // TODO
    }

    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public boolean hasContext() {
        return binding instanceof VariableType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol);
    }

    public boolean is(Type type) {
        return binding.simplify().equals(type.simplify());
    }

    public boolean isBound() {
        return !(binding instanceof VariableType);
    }

    @Override
    public Type qualifyNames(NameQualifier qualifier) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public String toString() {
        return toString_();
    }

    public InstanceType withBinding(Type binding) {
        return new InstanceType(symbol, binding);
    }

    @Override
    protected boolean contains(VariableType type) {
        return false;
    }

    @Override
    protected List<Type> flatten_() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    protected Set<Pair<VariableType, Symbol>> gatherContext_() {
        return ImmutableSet.of();
    }

    @Override
    protected Type genericCopy(TypeScope scope, Map<Type, Type> mappings) {
        return this;
    }

    @Override
    protected String getSignature_() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    protected String toParenthesizedString() {
        return toString_();
    }

    @Override
    protected String toString_() {
        return symbol.getSimpleName() + "<" + binding.toString_() + ">";
    }

    @Override
    protected Unification unifyWith(ConstructorType target, TypeScope scope) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    protected Unification unifyWith(FunctionType target, TypeScope scope) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Unification unifyWith(VariableType target, TypeScope scope) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Unification unifyWith(SumType target, TypeScope scope) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Unification unify_(Type type, TypeScope scope) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Optional<List<Pair<Type, Type>>> zip_(Type other, TypeScope scope) {
        throw new UnsupportedOperationException(); // TODO
    }
}
