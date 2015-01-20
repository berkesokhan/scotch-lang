package scotch.compiler.syntax;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static scotch.compiler.symbol.Operator.operator;
import static scotch.compiler.symbol.Symbol.fromString;
import static scotch.compiler.symbol.Symbol.qualified;
import static scotch.compiler.symbol.SymbolEntry.immutableEntry;
import static scotch.compiler.symbol.Type.fn;
import static scotch.compiler.symbol.Type.sum;
import static scotch.compiler.symbol.Type.var;
import static scotch.compiler.symbol.TypeClassDescriptor.typeClass;
import static scotch.compiler.symbol.Value.Fixity.LEFT_INFIX;
import static scotch.compiler.util.TestUtil.intType;
import static scotch.compiler.util.TestUtil.typeInstance;
import static scotch.data.tuple.TupleValues.tuple2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.google.common.collect.ImmutableSet;
import scotch.compiler.symbol.MethodSignature;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.SymbolEntry;
import scotch.compiler.symbol.SymbolEntry.ImmutableEntry;
import scotch.compiler.symbol.SymbolResolver;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.TypeInstanceDescriptor;
import scotch.data.eq.Eq;
import scotch.data.tuple.Tuple2;

public class StubResolver implements SymbolResolver {

    public static ImmutableEntry defaultEq() {
        Symbol symbol = fromString("scotch.data.eq.(==)");
        Type a = var("a", asList("scotch.data.eq.Eq"));
        return immutableEntry(symbol)
            .withValue(fn(a, fn(a, sum("scotch.data.bool.Bool"))))
            .withValueSignature(MethodSignature.fromMethod(Eq.class, "eq"))
            .withMemberOf(fromString("scotch.data.eq.Eq"))
            .withOperator(operator(LEFT_INFIX, 5))
            .build();
    }

    public static ImmutableEntry defaultEqClass() {
        Symbol symbol = fromString("scotch.data.eq.Eq");
        return immutableEntry(symbol)
            .withTypeClass(typeClass(symbol, asList(var("a")), asList(
                fromString("scotch.data.eq.(==)"),
                fromString("scotch.data.eq.(/=)")
            )))
            .build();
    }

    public static TypeInstanceDescriptor defaultEqOf(Type type) {
        return typeInstance(
            "scotch.data.eq",
            "scotch.data.eq.Eq",
            asList(type),
            mock(MethodSignature.class)
        );
    }

    public static ImmutableEntry defaultInt() {
        return immutableEntry(qualified("scotch.data.int", "Int"))
            .withType(intType())
            .build();
    }

    public static ImmutableEntry defaultMinus() {
        Type a = var("a", asList("scotch.data.num.Num"));
        return immutableEntry(qualified("scotch.data.num", "-"))
            .withOperator(operator(LEFT_INFIX, 6))
            .withValue(fn(a, fn(a, a)))
            .withMemberOf(fromString("scotch.data.num.Num"))
            .build();
    }

    public static ImmutableEntry defaultPlus() {
        Type a = var("a", asList("scotch.data.num.Num"));
        return immutableEntry(qualified("scotch.data.num", "+"))
            .withOperator(operator(LEFT_INFIX, 6))
            .withValue(fn(a, fn(a, a)))
            .withMemberOf(fromString("scotch.data.num.Num"))
            .build();
    }

    public static ImmutableEntry defaultString() {
        return immutableEntry(qualified("scotch.data.string", "String"))
            .withType(sum("scotch.data.string.String"))
            .build();
    }

    public static ImmutableEntry defaultNum() {
        Symbol symbol = fromString("scotch.data.num.Num");
        return immutableEntry(symbol)
            .withTypeClass(typeClass(symbol, asList(var("a")), asList(
                fromString("scotch.data.num.(+)"),
                fromString("scotch.data.num.(-)")
            )))
            .build();
    }

    public static TypeInstanceDescriptor defaultNumOf(Type type) {
        return typeInstance(
            "scotch.data.num",
            "scotch.data.num.Num",
            asList(type),
            mock(MethodSignature.class)
        );
    }

    private final Map<Symbol, SymbolEntry>                                     symbols;
    private final Map<Tuple2<Symbol, List<Type>>, Set<TypeInstanceDescriptor>> typeInstances;
    private final Map<Symbol, Set<TypeInstanceDescriptor>>                     typeInstancesByClass;
    private final Map<List<Type>, Set<TypeInstanceDescriptor>>                 typeInstancesByArguments;
    private final Map<String, Set<TypeInstanceDescriptor>>                     typeInstancesByModule;

    public StubResolver() {
        this.symbols = new HashMap<>();
        this.typeInstances = new HashMap<>();
        this.typeInstancesByClass = new HashMap<>();
        this.typeInstancesByArguments = new HashMap<>();
        this.typeInstancesByModule = new HashMap<>();
    }

    public StubResolver define(SymbolEntry entry) {
        symbols.put(entry.getSymbol(), entry);
        return this;
    }

    public StubResolver define(TypeInstanceDescriptor typeInstance) {
        typeInstances.computeIfAbsent(tuple2(typeInstance.getTypeClass(), typeInstance.getParameters()), k -> new HashSet<>()).add(typeInstance);
        typeInstancesByClass.computeIfAbsent(typeInstance.getTypeClass(), k -> new HashSet<>()).add(typeInstance);
        typeInstancesByArguments.computeIfAbsent(typeInstance.getParameters(), k -> new HashSet<>()).add(typeInstance);
        typeInstancesByModule.computeIfAbsent(typeInstance.getModuleName(), k -> new HashSet<>()).add(typeInstance);
        return this;
    }

    @Override
    public Set<TypeInstanceDescriptor> getTypeInstancesByModule(String moduleName) {
        return typeInstancesByModule.getOrDefault(moduleName, ImmutableSet.of());
    }

    @Override
    public boolean isDefined(Symbol symbol) {
        return symbols.containsKey(symbol);
    }

    @Override
    public Optional<SymbolEntry> getEntry(Symbol symbol) {
        return Optional.ofNullable(symbols.get(symbol));
    }

    @Override
    public Set<TypeInstanceDescriptor> getTypeInstances(Symbol symbol, List<Type> types) {
        return ImmutableSet.copyOf(typeInstances.getOrDefault(tuple2(symbol, types), ImmutableSet.of()));
    }
}
