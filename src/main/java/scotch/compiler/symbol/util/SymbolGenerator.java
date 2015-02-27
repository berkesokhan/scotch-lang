package scotch.compiler.symbol.util;

import static scotch.compiler.symbol.Symbol.unqualified;
import static scotch.compiler.symbol.type.Types.t;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.type.Types;
import scotch.compiler.symbol.type.VariableType;

public class SymbolGenerator {

    private final Map<List<String>, AtomicInteger> counters;
    private       int                              nextSymbol;
    private       int                              nextType;

    public SymbolGenerator() {
        counters = new HashMap<>();
    }

    public Symbol reserveSymbol() {
        return unqualified(String.valueOf(nextSymbol++));
    }

    public Symbol reserveSymbol(List<String> nestings) {
        List<String> memberNames = new ArrayList<>();
        memberNames.addAll(nestings);
        memberNames.add(String.valueOf(counters.computeIfAbsent(nestings, k -> new AtomicInteger()).getAndIncrement()));
        return unqualified(memberNames);
    }

    public VariableType reserveType() {
        return Types.t(nextType++);
    }
}
