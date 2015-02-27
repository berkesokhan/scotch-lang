package scotch.compiler.symbol.descriptor;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import com.google.common.collect.ImmutableSet;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.type.Type;

public class TypeClassDescriptor {

    public static TypeClassDescriptor typeClass(Symbol symbol, List<Type> arguments, Collection<Symbol> members) {
        return new TypeClassDescriptor(symbol, arguments, members);
    }

    private final Symbol      symbol;
    private final List<Type>  parameters;
    private final Set<Symbol> members;

    public TypeClassDescriptor(Symbol symbol, List<Type> parameters, Collection<Symbol> members) {
        this.symbol = symbol;
        this.parameters = parameters;
        this.members = ImmutableSet.copyOf(members);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof TypeClassDescriptor) {
            TypeClassDescriptor other = (TypeClassDescriptor) o;
            return Objects.equals(symbol, other.symbol)
                && Objects.equals(parameters, other.parameters)
                && Objects.equals(members, other.members);
        } else {
            return false;
        }
    }

    public List<Type> getParameters() {
        return parameters;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, parameters, members);
    }

    @Override
    public String toString() {
        return symbol.getSimpleName();
    }
}
