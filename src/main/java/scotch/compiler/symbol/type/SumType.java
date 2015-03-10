package scotch.compiler.symbol.type;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static scotch.compiler.symbol.type.Types.unifyVariable;
import static scotch.compiler.symbol.type.Unification.circular;
import static scotch.compiler.symbol.type.Unification.mismatch;
import static scotch.compiler.symbol.type.Unification.unified;
import static scotch.compiler.util.Pair.pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import lombok.EqualsAndHashCode;
import scotch.compiler.symbol.NameQualifier;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.text.SourceRange;
import scotch.compiler.util.Pair;
import scotch.runtime.Callable;

@EqualsAndHashCode(callSuper = false)
public class SumType extends Type {

    private static void shouldBeSumName(Symbol symbol) {
        if (!symbol.isSumName()) {
            throw new IllegalArgumentException("Sum type should have upper-case name, be tuple, or list: got '" + symbol.getMemberName() + "'");
        }
    }

    private static List<Pair<Type, Type>> zip(List<Type> left, List<Type> right) {
        List<Pair<Type, Type>> result = new ArrayList<>();
        Iterator<Type> leftIterator = left.iterator();
        Iterator<Type> rightIterator = right.iterator();
        while (leftIterator.hasNext()) {
            result.add(pair(leftIterator.next(), rightIterator.next()));
        }
        return result;
    }

    private final SourceRange sourceRange;
    private final Symbol      symbol;
    private final List<Type>  parameters;

    SumType(SourceRange sourceRange, Symbol symbol, List<Type> parameters) {
        shouldBeSumName(symbol);
        this.sourceRange = sourceRange;
        this.symbol = symbol;
        this.parameters = ImmutableList.copyOf(parameters);
    }

    @Override
    public void accept(Consumer<Symbol> consumer) {
        consumer.accept(symbol);
        parameters.forEach(parameter -> parameter.accept(consumer));
    }

    @Override
    public HeadApplication apply(Type head, TypeScope scope) {
        return head.applyWith(this, scope);
    }

    public HeadZip applyZip(Type head, TypeScope scope) {
        return head.applyZipWith(this, scope);
    }

    @Override
    public Type flatten() {
        return new SumType(sourceRange, symbol, parameters.stream()
            .map(Type::flatten)
            .collect(toList()));
    }

    @Override
    public Map<String, Type> getContexts(Type type, TypeScope scope) {
        return ImmutableMap.of();
    }

    @Override
    public Class<?> getJavaType() {
        return Callable.class;
    }

    public List<Type> getParameters() {
        return parameters;
    }

    @Override
    public String getSignature() {
        return "()L" + getSignature_() + ";";
    }

    @Override
    public SourceRange getSourceRange() {
        return sourceRange;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public Type qualifyNames(NameQualifier qualifier) {
        return withSymbol(qualifier.qualify(symbol)
            .orElseGet(() -> {
                qualifier.symbolNotFound(symbol, sourceRange);
                return symbol;
            }))
            .withParameters(parameters.stream()
                .map(argument -> argument.qualifyNames(qualifier))
                .collect(toList()));
    }

    public SumType withParameters(List<Type> arguments) {
        return new SumType(sourceRange, symbol, arguments);
    }

    public SumType withSourceRange(SourceRange sourceRange) {
        return new SumType(sourceRange, symbol, parameters);
    }

    public SumType withSymbol(Symbol symbol) {
        return new SumType(sourceRange, symbol, parameters);
    }

    @Override
    protected boolean contains(VariableType type) {
        return parameters.stream()
            .map(Type::simplify)
            .anyMatch(argument -> argument.equals(type));
    }

    @Override
    protected Type flatten(List<Type> types) {
        return withParameters(new ArrayList<Type>() {{
            addAll(parameters);
            addAll(types);
        }});
    }

    @Override
    protected List<Type> flatten_() {
        return ImmutableList.of(flatten());
    }

    @Override
    protected Set<Pair<VariableType, Symbol>> gatherContext_() {
        return parameters.stream()
            .flatMap(parameter -> parameter.gatherContext_().stream())
            .collect(toSet());
    }

    @Override
    protected Type generate(TypeScope scope, Set<Type> visited) {
        return withParameters(parameters.stream()
            .map(parameter -> parameter.generate(scope, visited))
            .collect(toList())).flatten();
    }

    @Override
    protected Type genericCopy(TypeScope scope, Map<Type, Type> mappings) {
        return new SumType(sourceRange, symbol, parameters.stream()
            .map(parameter -> parameter.genericCopy(scope, mappings))
            .collect(toList()));
    }

    @Override
    protected String getSignature_() {
        return symbol.getClassName();
    }

    @Override
    protected String toParenthesizedString() {
        return toString_();
    }

    @Override
    protected String toString_() {
        if (symbol.isTuple()) {
            return "(" + parameters.stream().map(Type::toString_).collect(joining(", ")) + ")";
        } else if (symbol.isList()) {
            return "[" + parameters.stream().map(Type::toString_).collect(joining(", ")) + "]";
        } else if (parameters.isEmpty()) {
            return symbol.getSimpleName();
        } else {
            return symbol.getSimpleName() + " " + parameters.stream().map(Type::toString_).collect(joining(" "));
        }
    }

    @Override
    protected Unification unifyWith(ConstructorType target, TypeScope scope) {
        return target.apply(this, scope);
    }

    @Override
    protected Unification unifyWith(SumType target, TypeScope scope) {
        if (symbol.equals(target.symbol)) {
            if (parameters.size() == target.parameters.size()) {
                List<Pair<Type, Type>> zip = zip(target.parameters, parameters);
                for (Pair<Type, Type> pair : zip) {
                    Unification result = pair.into((left, right) -> left.unify(right, scope));
                    if (!result.isUnified()) {
                        return result;
                    }
                }
                return unified(target);
            } else {
                return mismatch(target, this);
            }
        } else {
            return mismatch(target, this);
        }
    }

    @Override
    protected Unification unifyWith(FunctionType target, TypeScope scope) {
        return mismatch(target, this);
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
    protected Optional<List<Pair<Type, Type>>> zipWith(ConstructorType target, TypeScope scope) {
        return target.applyZip(this, scope);
    }

    @Override
    protected Optional<List<Pair<Type, Type>>> zipWith(SumType target, TypeScope scope) {
        if (equals(target)) {
            return Optional.of(ImmutableList.of(pair(target, this)));
        } else {
            return Optional.empty();
        }

    }

    @Override
    protected Optional<List<Pair<Type, Type>>> zip_(Type other, TypeScope scope) {
        return other.zipWith(this, scope);
    }
}
