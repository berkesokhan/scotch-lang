package scotch.compiler.symbol.type;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static scotch.compiler.symbol.Unification.unified;
import static scotch.compiler.util.Either.left;
import static scotch.compiler.util.Either.right;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import scotch.compiler.steps.NameQualifier;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.TypeScope;
import scotch.compiler.symbol.Unification;
import scotch.compiler.text.SourceRange;
import scotch.compiler.util.Either;
import scotch.compiler.util.Pair;

/**
 * Variable sum, where a sum can be applied and its parameters verified for compatibility.
 * <p>
 * <p>This class supports the concept of a <a href="http://en.wikipedia.org/wiki/Type_constructor">type constructor</a>
 * in simply typed lambda calculus, or a <a href="http://en.wikipedia.org/wiki/Kind_%28type_theory%29#Kinds_in_Haskell">kind</a>
 * in Haskell.</p>
 */
public class VariableSum extends Type {

    private final Type       variable;
    private final List<Type> parameters;

    public VariableSum(Type variable, List<Type> parameters) {
        this.variable = variable;
        this.parameters = ImmutableList.copyOf(parameters);
    }

    @Override
    public Unification apply(SumType sum, TypeScope scope) {
        return rebind(scope).map(
            varResult -> sum.rebind(scope).map(
                sumResult -> ((VariableSum) varResult).apply_(sum, scope)));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof VariableSum) {
            VariableSum other = (VariableSum) o;
            return Objects.equals(variable, other.variable)
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
        return Objects.hash(variable, parameters);
    }

    @Override
    public Type qualifyNames(NameQualifier qualifier) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public Unification rebind(TypeScope scope) {
        List<Type> resultParams = new ArrayList<>();
        for (Type parameter : parameters) {
            Unification result = parameter.rebind(scope);
            result.ifUnified(resultParams::add);
            if (!result.isUnified()) {
                return result;
            }
        }
        return unified(new VariableSum(variable, resultParams));
    }

    @Override
    public String toString() {
        return toString_();
    }

    @Override
    protected Optional<List<Pair<Type, Type>>> zip_(Type other) {
        return Optional.empty();
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
                    return variable.unify(sum, scope)
                        .map(unified -> unified(sum.withParameters(unifiedParams)));
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
    protected boolean contains(VariableType type) {
        return parameters.stream().anyMatch(parameter -> parameter.contains(type));
    }

    @Override
    protected Set<Pair<VariableType, Symbol>> gatherContext_() {
        Set<Pair<VariableType, Symbol>> context = new HashSet<>();
        parameters.forEach(parameter -> context.addAll(parameter.gatherContext_()));
        return ImmutableSortedSet.copyOf(Types::sort, context);
    }

    @Override
    protected Type generate(TypeScope scope, Set<Type> visited) {
        if (variable instanceof SumType) {
            return variable.generate(scope, visited);
        } else {
            return new VariableSum(
                variable.generate(scope, visited),
                parameters.stream()
                    .map(parameter -> parameter.generate(scope, visited))
                    .collect(toList()));
        }
    }

    @Override
    protected Type genericCopy(TypeScope scope, Map<Type, Type> mappings) {
        return new VariableSum(
            variable.genericCopy(scope, mappings),
            parameters.stream()
                .map(parameter -> parameter.genericCopy(scope, mappings))
                .collect(toList())
        );
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
        return variable + " " + parameters.stream()
            .map(Object::toString)
            .collect(joining(" "));
    }

    @Override
    protected Unification unifyWith(FunctionType target, TypeScope scope) {
        return unify_(target, scope);
    }

    @Override
    protected Unification unifyWith(VariableType target, TypeScope scope) {
        return unify_(target, scope);
    }

    @Override
    protected Unification unifyWith(SumType target, TypeScope scope) {
        return unify_(target, scope);
    }

    @Override
    protected Unification unifyWith(VariableSum target, TypeScope scope) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    protected Unification unify_(Type type, TypeScope scope) {
        return type.unifyWith(this, scope);
    }
}
