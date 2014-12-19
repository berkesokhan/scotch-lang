package scotch.compiler.symbol;

import static scotch.compiler.symbol.Symbol.qualified;
import static scotch.compiler.symbol.Symbol.unqualified;
import static scotch.compiler.symbol.Type.t;

import scotch.compiler.symbol.Type.VariableType;

public class SymbolGenerator {

    private int nextSymbol;
    private int nextType;

    public Symbol reserveSymbol() {
        return unqualified("$" + nextSymbol++);
    }

    public Symbol reserveSymbol(String moduleName) {
        return qualified(moduleName, "$" + nextSymbol++);
    }

    public VariableType reserveType() {
        return t(nextType++);
    }
}
