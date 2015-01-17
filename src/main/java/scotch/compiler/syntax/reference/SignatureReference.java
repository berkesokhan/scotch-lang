package scotch.compiler.syntax.reference;

import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import scotch.compiler.symbol.Symbol;

public class SignatureReference extends DefinitionReference {

    private final Symbol symbol;

    SignatureReference(Symbol symbol) {
        this.symbol = symbol;
    }

    @Override
    public <T> T accept(DefinitionReferenceVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof SignatureReference && Objects.equals(symbol, ((SignatureReference) o).symbol);
    }

    public String getMemberName() {
        return symbol.getMemberName();
    }

    public String getName() {
        return symbol.getMemberName();
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
