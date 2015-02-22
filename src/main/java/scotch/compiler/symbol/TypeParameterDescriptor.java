package scotch.compiler.symbol;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.symbol.type.Types.sum;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.google.common.collect.ImmutableList;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import scotch.compiler.symbol.type.SumType;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.syntax.scope.Scope;

@EqualsAndHashCode
@ToString
public class TypeParameterDescriptor {

    public static TypeParameterDescriptor typeParam(Type type) {
        if (type instanceof SumType) {
            return fromType_((SumType) type);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static TypeParameterDescriptor fromType_(SumType type) {
        return new TypeParameterDescriptor(type.getSymbol(), type.getParameters().stream()
            .map(Type::getContext)
            .collect(toList()));
    }

    private final Symbol            symbol;
    private final List<Set<Symbol>> argumentContexts;

    private TypeParameterDescriptor(Symbol symbol, List<Set<Symbol>> argumentContexts) {
        this.symbol = symbol;
        this.argumentContexts = ImmutableList.copyOf(argumentContexts.stream()
            .map(HashSet<Symbol>::new)
            .collect(toList()));
    }

    public Set<Symbol> getContext() {
        Set<Symbol> context = new HashSet<>();
        argumentContexts.forEach(context::addAll);
        return context;
    }

    public boolean matches(Type type) {
        return type instanceof SumType && matches_((SumType) type);
    }

    public Type reify(Scope scope) {
        return sum(symbol, argumentContexts.stream()
            .map(context -> scope.reserveType().withContext(context))
            .collect(toList()));
    }

    private boolean matches_(SumType type) {
        if (type.getSymbol().equals(symbol)) {
            List<Set<Symbol>> otherContexts = type.getParameters().stream()
                .map(Type::getContext)
                .collect(toList());
            if (otherContexts.size() == argumentContexts.size()) {
                for (int i = 0; i < argumentContexts.size(); i++) {
                    if (!otherContexts.get(0).containsAll(argumentContexts.get(0))) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }
}
