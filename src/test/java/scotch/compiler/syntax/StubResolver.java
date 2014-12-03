package scotch.compiler.syntax;

import static scotch.compiler.symbol.Value.Fixity.LEFT_INFIX;
import static scotch.compiler.symbol.Operator.operator;
import static scotch.compiler.symbol.Symbol.qualified;
import static scotch.compiler.symbol.SymbolEntry.immutableEntry;
import static scotch.compiler.symbol.Type.fn;
import static scotch.compiler.symbol.Type.sum;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.SymbolEntry;
import scotch.compiler.symbol.SymbolResolver;

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
    public Optional<SymbolEntry> getEntry(Symbol symbol) {
        return Optional.ofNullable(symbols.get(symbol));
    }
}
