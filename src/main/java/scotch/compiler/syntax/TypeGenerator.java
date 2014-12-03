package scotch.compiler.syntax;

import static scotch.compiler.symbol.Symbol.qualified;
import static scotch.compiler.symbol.Type.t;

import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;

public class TypeGenerator {

    private int nextId;

    public Symbol reservePattern(String moduleName) {
        return qualified(moduleName, "pattern#" + nextId++);
    }

    public Type reserveType() {
        return t(nextId++);
    }
}
