package scotch.compiler.symbol.type;

import static me.qmx.jitescript.util.CodegenUtils.p;
import static scotch.compiler.symbol.Unification.circular;
import static scotch.compiler.symbol.Unification.mismatch;
import static scotch.compiler.symbol.Unification.unified;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import com.google.common.collect.ImmutableSortedSet;
import scotch.compiler.steps.NameQualifier;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.TypeScope;
import scotch.compiler.symbol.Unification;
import scotch.compiler.text.SourceRange;
import scotch.data.tuple.Tuple2;
import scotch.runtime.Applicable;

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
    public <T> T accept(TypeVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public Unification apply(SumType sum, TypeScope scope) {
        return argument.apply(sum, scope)
            .map(argResult -> result.apply(sum, scope)
                .map(resultResult -> unified(withArgument(argResult).withResult(resultResult))));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof FunctionType) {
            FunctionType other = (FunctionType) o;
            return argument.equals(other.argument) && result.equals(other.result);
        } else {
            return false;
        }
    }

    public Type getArgument() {
        return argument;
    }

    @Override
    public Map<String, Type> getContexts(Type type, TypeScope scope) {
        Map<String, Type> map = new HashMap<>();
        type.accept(new TypeVisitor<Void>() {
            @Override
            public Void visit(FunctionType type) {
                map.putAll(argument.getContexts(type.getArgument(), scope));
                map.putAll(result.getContexts(type.getResult(), scope));
                return null;
            }

            @Override
            public Void visitOtherwise(Type type) {
                return null;
            }
        });
        return map;
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
    public int hashCode() {
        return Objects.hash(argument, result);
    }

    @Override
    public Type qualifyNames(NameQualifier qualifier) {
        return withArgument(argument.qualifyNames(qualifier)).withResult(result.qualifyNames(qualifier));
    }

    @Override
    public Type rebind(TypeScope scope) {
        return withArgument(argument.rebind(scope))
            .withResult(argument.rebind(scope));
    }

    @Override
    public String toString() {
        return gatherContext() + toString_();
    }

    @Override
    public Unification unify(Type type, TypeScope scope) {
        return type.unifyWith(this, scope);
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
    protected Set<Tuple2<VariableType, Symbol>> gatherContext_() {
        Set<Tuple2<VariableType, Symbol>> context = new HashSet<>();
        context.addAll(argument.gatherContext_());
        context.addAll(result.gatherContext_());
        return ImmutableSortedSet.copyOf(Type::sort, context);
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
    protected Unification unifyWith(SumType target, TypeScope scope) {
        return mismatch(target, this);
    }

    @Override
    protected Unification unifyWith(FunctionType target, TypeScope scope) {
        return target.argument.unify(argument, scope).map(
            argumentResult -> target.result.unify(result, scope).map(
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
}
