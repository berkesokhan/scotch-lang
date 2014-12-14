package scotch.compiler.symbol;

import static scotch.compiler.symbol.Symbol.qualified;
import static scotch.compiler.symbol.Type.t;

import scotch.compiler.symbol.Type.VariableType;

public class TypeGenerator {

    private int nextId;

    public Symbol reservePattern(String moduleName) {
        return qualified(moduleName, "pattern#" + nextId++);
    }

    public VariableType reserveType() {
        return t(nextId++);
    }
}
