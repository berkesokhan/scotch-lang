package scotch.compiler.symbol.type;

import static java.util.stream.Collectors.joining;
import static scotch.compiler.symbol.Unification.unified;
import static scotch.data.either.Either.left;
import static scotch.data.either.Either.right;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import scotch.compiler.steps.NameQualifier;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.TypeScope;
import scotch.compiler.symbol.Unification;
import scotch.compiler.text.SourceRange;
import scotch.data.either.Either;
import scotch.data.tuple.Tuple2;

/**
 * Variable sum, where a sum can be applied and its parameters verified for compatibility.
 *
 * <p>This class supports the concept of a <a href="http://en.wikipedia.org/wiki/Type_constructor">type constructor</a>
 * in simply typed lambda calculus, or a <a href="http://en.wikipedia.org/wiki/Kind_%28type_theory%29#Kinds_in_Haskell">kind</a>
 * in Haskell.</p>
 */
public class VariableSum extends Type {

    private final String             name;
    private final List<VariableType> parameters;

    public VariableSum(String name, List<VariableType> parameters) {
        this.name = name;
        this.parameters = ImmutableList.copyOf(parameters);
    }

    @Override
    public <T> T accept(TypeVisitor<T> visitor) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public Unification apply(SumType sum, TypeScope scope) {
        return rebind(scope).map(
            varResult -> sum.rebind(scope).map(
                sumResult -> ((VariableSum) varResult).apply_(sum, scope)));
    }

    private Unification apply_(SumType sum, TypeScope scope) {
        return getSumParams(sum, scope)
            .map(sumParams -> {
                List<Type> compareSum = new ArrayList<>();
                List<Type> compareVar = new ArrayList<>();
                List<Type> remainder = new ArrayList<>();
                List<Type> unifiedParams = new ArrayList<>();
                if (sumParams.size() > parameters.size()) {
                    compareVar.addAll(parameters);
                    compareSum.addAll(sumParams.subList(0, parameters.size()));
                    remainder.addAll(sumParams.subList(parameters.size(), sumParams.size()));
                } else if (sumParams.size() < parameters.size()) {
                    compareVar.addAll(parameters.subList(0, sumParams.size()));
                    compareSum.addAll(sumParams);
                    remainder.addAll(parameters.subList(sumParams.size(), parameters.size()));
                } else {
                    compareVar.addAll(parameters);
                    compareSum.addAll(sumParams);
                }
                for (int i = 0; i < compareVar.size(); i++) {
                    Unification result = compareVar.get(i).unify(compareSum.get(i), scope);
                    if (result.isUnified()) {
                        result.ifUnified(unifiedParams::add);
                    } else {
                        return result;
                    }
                }
                unifiedParams.addAll(remainder);
                if (unifiedParams.size() > scope.getDataParameters(sum).size()) {
                    throw new UnsupportedOperationException(); // TODO
                } else {
                    return unified(sum.withParameters(unifiedParams));
                }
            })
            .orElseGet(left -> left);
    }

    private Either<Unification, List<Type>> getSumParams(SumType sum, TypeScope scope) {
        List<Type> sumParams = new ArrayList<>(sum.getParameters());
        Either<Unification, List<Type>> dataParams = getSumParams_(scope.getDataParameters(sum), scope);
        return dataParams.map(right -> {
            if (sumParams.size() < right.size()) {
                sumParams.addAll(right.subList(sumParams.size(), right.size()));
            }
            return sumParams;
        });
    }

    private Either<Unification, List<Type>> getSumParams_(List<Type> parameters, TypeScope scope) {
        List<Type> resultParams = new ArrayList<>();
        for (Type parameter : parameters) {
            Unification result = parameter.rebind(scope);
            if (!result.isUnified()) {
                return left(result);
            }
            resultParams.add(result
                .map(type -> type.rebind(scope))
                .orElseThrow(unification -> new IllegalStateException(unification.prettyPrint())));
        }
        return right(resultParams);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof VariableSum) {
            VariableSum other = (VariableSum) o;
            return Objects.equals(name, other.name)
                && Objects.equals(parameters, other.parameters);
        } else {
            return false;
        }
    }

    @Override
    public Map<String, Type> getContexts(Type type, TypeScope scope) {
        throw new UnsupportedOperationException(); // TODO
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

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public Type qualifyNames(NameQualifier qualifier) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public Unification rebind(TypeScope scope) {
        List<VariableType> resultParams = new ArrayList<>();
        for (VariableType parameter : parameters) {
            Unification result = parameter.rebind(scope);
            result.ifUnified(type -> resultParams.add((VariableType) type));
            if (!result.isUnified()) {
                return result;
            }
        }
        return unified(withParameters(resultParams));
    }

    private VariableSum withParameters(List<VariableType> parameters) {
        return new VariableSum(name, parameters);
    }

    @Override
    public String toString() {
        return toString_();
    }

    @Override
    public Unification unify(Type type, TypeScope scope) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    protected boolean contains(VariableType type) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    protected Set<Tuple2<VariableType, Symbol>> gatherContext_() {
        Set<Tuple2<VariableType, Symbol>> context = new HashSet<>();
        parameters.forEach(parameter -> context.addAll(parameter.gatherContext_()));
        return ImmutableSortedSet.copyOf(Type::sort, context);
    }

    @Override
    protected String getSignature_() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    protected String toParenthesizedString() {
        return "(" + toString_() + ")";
    }

    @Override
    protected String toString_() {
        return name + " " + parameters.stream()
            .map(Object::toString)
            .collect(joining(" "));
    }

    @Override
    protected Unification unifyWith(FunctionType target, TypeScope scope) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    protected Unification unifyWith(VariableType target, TypeScope scope) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    protected Unification unifyWith(SumType target, TypeScope scope) {
        throw new UnsupportedOperationException(); // TODO
    }
}
