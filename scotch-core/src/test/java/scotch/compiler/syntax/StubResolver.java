package scotch.compiler.syntax;

import static scotch.compiler.syntax.Operator.Fixity.LEFT_INFIX;
import static scotch.compiler.syntax.Operator.operator;
import static scotch.compiler.syntax.Symbol.qualified;
import static scotch.compiler.syntax.SymbolEntry.immutableEntry;
import static scotch.compiler.syntax.Type.fn;
import static scotch.compiler.syntax.Type.sum;

import java.util.HashMap;
import java.util.Map;

public class StubResolver implements SymbolResolver {

    public static SymbolEntry defaultInt() {
        return immutableEntry(qualified("scotch.data.int", "Int"))
            .withType(sum("scotch.data.int.Int"))
            .build();
    }

    public static SymbolEntry defaultMinus() {
        return immutableEntry(qualified("scotch.data.num", "-"))
            .withOperator(operator(LEFT_INFIX, 6))
            .withValue(fn(sum("scotch.data.int.Int"), fn(sum("scotch.data.int.Int"), sum("scotch.data.int.Int"))))
            .build();
    }

    public static SymbolEntry defaultPlus() {
        return immutableEntry(qualified("scotch.data.num", "+"))
            .withOperator(operator(LEFT_INFIX, 6))
            .withValue(fn(sum("scotch.data.int.Int"), fn(sum("scotch.data.int.Int"), sum("scotch.data.int.Int"))))
            .build();
    }

    public static SymbolEntry defaultString() {
        return immutableEntry(qualified("scotch.data.string", "String"))
            .withType(sum("scotch.data.string.String"))
            .build();
    }

    private final Map<Symbol, SymbolEntry> symbols = new HashMap<>();

    public StubResolver define(SymbolEntry entry) {
        symbols.put(entry.getSymbol(), entry);
        return this;
    }

    @Override
    public boolean isDefined(Symbol symbol) {
        return symbols.containsKey(symbol);
    }

    @Override
    public SymbolEntry getEntry(Symbol symbol) {
        return symbols.get(symbol);
    }
}
