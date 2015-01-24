package scotch.compiler.syntax.reference;

import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import scotch.compiler.symbol.Symbol;

public class ScopeReference extends DefinitionReference {

    private final Symbol symbol;

    ScopeReference(Symbol symbol) {
        this.symbol = symbol;
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof ScopeReference && Objects.equals(symbol, ((ScopeReference) o).symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol);
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + symbol.getCanonicalName() + ")";
    }
}
