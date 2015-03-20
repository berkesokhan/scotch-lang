package scotch.symbol.type;

import static lombok.AccessLevel.PACKAGE;
import static scotch.symbol.type.Unification.mismatch;
import static scotch.symbol.type.Unification.unified;
import static scotch.symbol.type.Types.unifyVariable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import scotch.symbol.NameQualifier;
import scotch.symbol.Symbol;
import scotch.compiler.text.SourceLocation;
import scotch.compiler.util.Pair;

@AllArgsConstructor(access = PACKAGE)
@EqualsAndHashCode(callSuper = false)
public class ConstructorType extends Type {

    private final Type head;
    private final Type tail;

    @Override
    public Type flatten() {
        return head.flatten(tail.flatten_());
    }

    @Override
    public Map<String, Type> getContexts(Type type, TypeScope scope) {
        return new HashMap<String, Type>() {{
            putAll(head.getContexts(type, scope));
            putAll(tail.getContexts(type, scope));
        }};
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
    public SourceLocation getSourceLocation() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public Type qualifyNames(NameQualifier qualifier) {
        throw new UnsupportedOperationException(); // TODO
    }

    protected Unification apply(SumType type, TypeScope scope) {
        return type.apply(head, scope).unify(
            (appliedType, remainingParameters) -> {
                if (remainingParameters.isEmpty()) {
                    return unified(new ConstructorType(appliedType, tail).flatten());
                } else {
                    return tail.apply(appliedType, remainingParameters, scope);
                }
            });
    }

    protected Optional<List<Pair<Type, Type>>> applyZip(SumType type, TypeScope scope) {
        return type.applyZip(head, scope).zip(
            (zippedPair, remainingParameters) -> {
                if (remainingParameters.isEmpty()) {
                    return Optional.of(new ArrayList<Pair<Type, Type>>() {{
                        add(zippedPair);
                    }});
                } else {
                    return tail.applyZip(zippedPair, remainingParameters, scope);
                }
            });
    }

    @Override
    protected boolean contains(VariableType type) {
        return head.contains(type) || tail.contains(type);
    }

    @Override
    protected List<Type> flatten_() {
        return new ArrayList<Type>() {{
            addAll(head.flatten_());
            addAll(tail.flatten_());
        }};
    }

    @Override
    protected Set<Pair<VariableType, Symbol>> gatherContext_() {
        return new HashSet<Pair<VariableType, Symbol>>() {{
            addAll(head.gatherContext_());
            addAll(tail.gatherContext_());
        }};
    }

    @Override
    protected Type generate(TypeScope scope, Set<Type> visited) {
        return new ConstructorType(head.generate(scope, visited), tail.generate(scope, visited)).flatten();
    }

    @Override
    protected Type genericCopy(TypeScope scope, Map<Type, Type> mappings) {
        return new ConstructorType(
            head.genericCopy(scope, mappings),
            tail.genericCopy(scope, mappings)
        );
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
        return "Λ(" + head.toString_() + ", " + tail.toString_() + ")";
    }

    @Override
    protected Unification unifyWith(ConstructorType target, TypeScope scope) {
        return head.unify(target.head, scope)
            .map(checkedHead -> tail.unify(target.tail, scope)
                .map(checkedTail -> unified(new ConstructorType(checkedHead, checkedTail))));
    }

    @Override
    protected Unification unifyWith(FunctionType target, TypeScope scope) {
        return mismatch(target, this);
    }

    @Override
    protected Unification unifyWith(VariableType target, TypeScope scope) {
        return unifyVariable(this, target, scope);
    }

    @Override
    protected Unification unifyWith(SumType target, TypeScope scope) {
        return flatten().unify_(target, scope); // TODO
    }

    @Override
    protected Unification unify_(Type type, TypeScope scope) {
        return type.unifyWith(this, scope);
    }

    @Override
    protected Optional<List<Pair<Type, Type>>> zipWith(ConstructorType target, TypeScope scope) {
        return target.head.zip_(head, scope)
            .flatMap(headZip -> target.tail.zip_(tail, scope)
                .flatMap(tailZip -> Optional.of(new ArrayList<Pair<Type, Type>>() {{
                    addAll(headZip);
                    addAll(tailZip);
                }})));
    }

    @Override
    protected Optional<List<Pair<Type, Type>>> zip_(Type other, TypeScope scope) {
        return other.zipWith(this, scope);
    }
}
