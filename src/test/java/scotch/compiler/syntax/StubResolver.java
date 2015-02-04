package scotch.compiler.syntax;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static scotch.compiler.symbol.Operator.operator;
import static scotch.compiler.symbol.Symbol.qualified;
import static scotch.compiler.symbol.Symbol.symbol;
import static scotch.compiler.symbol.SymbolEntry.immutableEntry;
import static scotch.compiler.symbol.TypeClassDescriptor.typeClass;
import static scotch.compiler.symbol.Value.Fixity.LEFT_INFIX;
import static scotch.compiler.symbol.type.Types.fn;
import static scotch.compiler.symbol.type.Types.sum;
import static scotch.compiler.symbol.type.Types.var;
import static scotch.compiler.symbol.type.Types.varSum;
import static scotch.compiler.util.Pair.pair;
import static scotch.compiler.util.TestUtil.intType;
import static scotch.compiler.util.TestUtil.typeInstance;

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
import scotch.compiler.symbol.SymbolEntry.ImmutableEntryBuilder;
import scotch.compiler.symbol.SymbolResolver;
import scotch.compiler.symbol.TypeInstanceDescriptor;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.util.Pair;

public class StubResolver implements SymbolResolver {

    public static ImmutableEntry defaultBind() {
        Symbol symbol = symbol("scotch.control.monad.(>>=)");
        return immutableEntry(symbol)
            .withValueType(fn(varSum("m", var("a")), fn(fn(var("a"), varSum("m", var("b"))), varSum("m", var("b")))))
            .withMemberOf(symbol("scotch.control.monad.Monad"))
            .withOperator(operator(LEFT_INFIX, 1))
            .build();
    }

    public static ImmutableEntry defaultEither() {
        Symbol symbol = symbol("scotch.data.either.Either");
        ImmutableEntryBuilder builder = immutableEntry(symbol)
            .withType(sum(symbol, asList(var("a"), var("b"))));
        builder.dataType()
            .withClassName("scotch/data/either/Either")
            .withParameters(asList(var("a"), var("b")))
            .withConstructors(asList());
        return builder.build();
    }

    public static ImmutableEntry defaultLeft() {
        Symbol symbol = symbol("scotch.data.either.Left");
        return immutableEntry(symbol)
            .withValueType(fn(var("a"), sum(symbol("scotch.data.either.Either"), asList(var("a"), var("b")))))
            .build();
    }

    public static ImmutableEntry defaultRight() {
        Symbol symbol = symbol("scotch.data.either.Right");
        return immutableEntry(symbol)
            .withValueType(fn(var("b"), sum(symbol("scotch.data.either.Either"), asList(var("a"), var("b")))))
            .build();
    }

    public static ImmutableEntry defaultEq() {
        Symbol symbol = symbol("scotch.data.eq.(==)");
        Type a = var("a", asList("scotch.data.eq.Eq"));
        return immutableEntry(symbol)
            .withValueType(fn(a, fn(a, sum("scotch.data.bool.Bool"))))
            .withMemberOf(symbol("scotch.data.eq.Eq"))
            .withOperator(operator(LEFT_INFIX, 5))
            .build();
    }

    public static ImmutableEntry defaultEqClass() {
        Symbol symbol = symbol("scotch.data.eq.Eq");
        return immutableEntry(symbol)
            .withTypeClass(typeClass(symbol, asList(var("a")), asList(
                symbol("scotch.data.eq.(==)"),
                symbol("scotch.data.eq.(/=)")
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

    public static ImmutableEntry defaultFromInteger() {
        Type a = var("a", asList("scotch.data.num.Num"));
        return immutableEntry(qualified("scotch.data.num", "fromInteger"))
            .withValueType(fn(intType(), a))
            .withMemberOf(symbol("scotch.data.num.Num"))
            .build();
    }

    public static ImmutableEntry defaultMinus() {
        Type a = var("a", asList("scotch.data.num.Num"));
        return immutableEntry(qualified("scotch.data.num", "-"))
            .withOperator(operator(LEFT_INFIX, 6))
            .withValueType(fn(a, fn(a, a)))
            .withMemberOf(symbol("scotch.data.num.Num"))
            .build();
    }

    public static ImmutableEntry defaultMonad() {
        Symbol symbol = symbol("scotch.control.monad.Monad");
        return immutableEntry(symbol)
            .withTypeClass(typeClass(symbol, asList(var("m")), asList(
                symbol("scotch.control.monad.(>>=)"),
                symbol("scotch.control.monad.(>>)"),
                symbol("scotch.control.monad.return")
            )))
            .build();
    }

    public static TypeInstanceDescriptor defaultMonadOf(Type type) {
        return typeInstance(
            "scotch.control.monad",
            "scotch.control.monad.Monad",
            asList(type),
            mock(MethodSignature.class)
        );
    }

    public static ImmutableEntry defaultNum() {
        Symbol symbol = symbol("scotch.data.num.Num");
        return immutableEntry(symbol)
            .withTypeClass(typeClass(symbol, asList(var("a")), asList(
                symbol("scotch.data.num.(+)"),
                symbol("scotch.data.num.(-)")
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

    public static ImmutableEntry defaultPlus() {
        Type a = var("a", asList("scotch.data.num.Num"));
        return immutableEntry(qualified("scotch.data.num", "+"))
            .withOperator(operator(LEFT_INFIX, 6))
            .withValueType(fn(a, fn(a, a)))
            .withMemberOf(symbol("scotch.data.num.Num"))
            .build();
    }

    public static ImmutableEntry defaultString() {
        return immutableEntry(qualified("scotch.data.string", "String"))
            .withType(sum("scotch.data.string.String"))
            .build();
    }

    private final Map<Symbol, SymbolEntry>                                   symbols;
    private final Map<Pair<Symbol, List<Type>>, Set<TypeInstanceDescriptor>> typeInstances;
    private final Map<Symbol, Set<TypeInstanceDescriptor>>                   typeInstancesByClass;
    private final Map<List<Type>, Set<TypeInstanceDescriptor>>               typeInstancesByArguments;
    private final Map<String, Set<TypeInstanceDescriptor>>                   typeInstancesByModule;

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
        typeInstances.computeIfAbsent(pair(typeInstance.getTypeClass(), typeInstance.getParameters()), k -> new HashSet<>()).add(typeInstance);
        typeInstancesByClass.computeIfAbsent(typeInstance.getTypeClass(), k -> new HashSet<>()).add(typeInstance);
        typeInstancesByArguments.computeIfAbsent(typeInstance.getParameters(), k -> new HashSet<>()).add(typeInstance);
        typeInstancesByModule.computeIfAbsent(typeInstance.getModuleName(), k -> new HashSet<>()).add(typeInstance);
        return this;
    }

    @Override
    public Optional<SymbolEntry> getEntry(Symbol symbol) {
        return Optional.ofNullable(symbols.get(symbol));
    }

    @Override
    public Set<TypeInstanceDescriptor> getTypeInstances(Symbol symbol, List<Type> types) {
        return ImmutableSet.copyOf(typeInstances.getOrDefault(pair(symbol, types), ImmutableSet.of()));
    }

    @Override
    public Set<TypeInstanceDescriptor> getTypeInstancesByModule(String moduleName) {
        return typeInstancesByModule.getOrDefault(moduleName, ImmutableSet.of());
    }

    @Override
    public boolean isDefined(Symbol symbol) {
        return symbols.containsKey(symbol);
    }
}
