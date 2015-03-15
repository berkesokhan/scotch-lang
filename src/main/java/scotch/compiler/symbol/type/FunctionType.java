package scotch.compiler.symbol.type;

import static me.qmx.jitescript.util.CodegenUtils.p;
import static scotch.compiler.symbol.type.Unification.circular;
import static scotch.compiler.symbol.type.Unification.mismatch;
import static scotch.compiler.symbol.type.Unification.unified;
import static scotch.compiler.symbol.type.Types.fn;
import static scotch.compiler.symbol.type.Types.unifyVariable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import lombok.EqualsAndHashCode;
import scotch.compiler.symbol.NameQualifier;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.text.SourceRange;
import scotch.compiler.util.Pair;
import scotch.runtime.Applicable;

@EqualsAndHashCode(callSuper = false)
public class FunctionType extends Type {

    private final SourceRange sourceRange;
    private final Type        argument;
    private final Type        result;

    FunctionType(SourceRange sourceRange, Type argument, Type result) {
        this.sourceRange = sourceRange;
        this.argument = argument;
        this.result = result;
    }

    @Override
    public void accept(Consumer<Symbol> consumer) {
        argument.accept(consumer);
        result.accept(consumer);
    }

    @Override
    public Type flatten() {
        return new FunctionType(sourceRange, argument.flatten(), result.flatten());
    }

    public Type getArgument() {
        return argument;
    }

    @Override
    public Map<String, Type> getContexts(Type type, TypeScope scope) {
        Map<String, Type> map = new HashMap<>();
        if (type instanceof FunctionType) {
            map.putAll(argument.getContexts(((FunctionType) type).getArgument(), scope));
            map.putAll(result.getContexts(((FunctionType) type).getResult(), scope));
        }
        return map;
    }

    @Override
    public List<Pair<VariableType, Symbol>> getInstanceMap() {
        List<Pair<VariableType, Symbol>> instances = new ArrayList<>();
        instances.addAll(argument.getInstanceMap());
        instances.addAll(result.getInstanceMap());
        return instances;
    }

    @Override
    public Class<?> getJavaType() {
        return Applicable.class;
    }

    public Type getResult() {
        return result;
    }

    @Override
    public String getSignature() {
        return "(" + argument.getSignature_() + ");" + result.getSignature_();
    }

    @Override
    public SourceRange getSourceRange() {
        return sourceRange;
    }

    @Override
    public Type qualifyNames(NameQualifier qualifier) {
        return withArgument(argument.qualifyNames(qualifier)).withResult(result.qualifyNames(qualifier));
    }

    public FunctionType withArgument(Type argument) {
        return new FunctionType(sourceRange, argument, result);
    }

    public FunctionType withResult(Type result) {
        return new FunctionType(sourceRange, argument, result);
    }

    public FunctionType withSourceRange(SourceRange sourceRange) {
        return new FunctionType(sourceRange, argument, result);
    }

    @Override
    protected boolean contains(VariableType type) {
        return argument.contains(type) || result.contains(type);
    }

    @Override
    protected List<Type> flatten_() {
        return ImmutableList.of(flatten());
    }

    @Override
    protected Set<Pair<VariableType, Symbol>> gatherContext_() {
        Set<Pair<VariableType, Symbol>> context = new HashSet<>();
        context.addAll(argument.gatherContext_());
        context.addAll(result.gatherContext_());
        return ImmutableSortedSet.copyOf(Types::sort, context);
    }

    @Override
    protected Type generate(TypeScope scope, Set<Type> visited) {
        return new FunctionType(sourceRange, argument.generate(scope), result.generate(scope)).flatten();
    }

    @Override
    protected Type genericCopy(TypeScope scope, Map<Type, Type> mappings) {
        return new FunctionType(
            sourceRange,
            argument.genericCopy(scope, mappings),
            result.genericCopy(scope, mappings)
        );
    }

    @Override
    protected String getSignature_() {
        return p(Function.class);
    }

    @Override
    protected String toParenthesizedString() {
        return "(" + argument.toParenthesizedString() + " -> " + result.toString_() + ")";
    }

    @Override
    protected String toString_() {
        return argument.toParenthesizedString() + " -> " + result.toString_();
    }

    @Override
    protected Unification unifyWith(ConstructorType target, TypeScope scope) {
        return mismatch(target, this);
    }

    @Override
    protected Unification unifyWith(SumType target, TypeScope scope) {
        return mismatch(target, this);
    }

    @Override
    protected Unification unifyWith(FunctionType target, TypeScope scope) {
        return target.argument.unify_(argument, scope).map(
            argumentResult -> target.result.unify_(result, scope).map(
                resultResult -> unified(fn(argumentResult, resultResult))
            )
        );
    }

    @Override
    protected Unification unifyWith(VariableType target, TypeScope scope) {
        if (contains(target)) {
            return circular(target, this);
        } else {
            return unifyVariable(this, target, scope);
        }
    }

    @Override
    protected Unification unify_(Type type, TypeScope scope) {
        return type.unifyWith(this, scope);
    }

    @Override
    protected Optional<List<Pair<Type, Type>>> zipWith(FunctionType target, TypeScope scope) {
        return target.argument.zip_(argument, scope).flatMap(
            argumentList -> target.result.zip_(result, scope).map(
                resultList -> new ArrayList<Pair<Type, Type>>() {{
                    addAll(argumentList);
                    addAll(resultList);
                }}));
    }

    @Override
    protected Optional<List<Pair<Type, Type>>> zip_(Type other, TypeScope scope) {
        return other.zipWith(this, scope);
    }
}
