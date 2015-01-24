package scotch.compiler.syntax.reference;

import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import scotch.compiler.symbol.Symbol;

public class DataReference extends DefinitionReference {

    private final Symbol symbol;

    public DataReference(Symbol symbol) {
        this.symbol = symbol;
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof DataReference && Objects.equals(symbol, ((DataReference) o).symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol);
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + symbol.toString() + ")";
    }
}
