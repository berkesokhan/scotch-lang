package scotch.compiler.syntax.reference;

import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import scotch.compiler.symbol.Symbol;

public class ClassReference extends DefinitionReference {

    private final Symbol symbol;

    ClassReference(Symbol symbol) {
        this.symbol = symbol;
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof ClassReference && Objects.equals(symbol, ((ClassReference) o).symbol);
    }

    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol);
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + symbol + ")";
    }
}
