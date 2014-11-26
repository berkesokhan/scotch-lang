package scotch.compiler.syntax;

import static scotch.compiler.syntax.Symbol.qualified;
import static scotch.compiler.syntax.Type.t;

public class TypeGenerator {

    private int nextId;

    public Symbol reservePattern(String moduleName) {
        return qualified(moduleName, "pattern#" + nextId++);
    }

    public Type reserveType() {
        return t(nextId++);
    }
}
