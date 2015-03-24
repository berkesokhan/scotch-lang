package scotch.compiler.syntax.scope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import scotch.compiler.syntax.pattern.PatternCase;
import scotch.compiler.syntax.reference.ClassReference;
import scotch.symbol.MethodSignature;
import scotch.symbol.Operator;
import scotch.symbol.Symbol;
import scotch.symbol.SymbolEntry;
import scotch.symbol.SymbolResolver;
import scotch.symbol.descriptor.DataConstructorDescriptor;
import scotch.symbol.descriptor.DataTypeDescriptor;
import scotch.symbol.descriptor.TypeClassDescriptor;
import scotch.symbol.descriptor.TypeInstanceDescriptor;
import scotch.symbol.type.SumType;
import scotch.symbol.type.Type;
import scotch.symbol.type.TypeScope;
import scotch.symbol.type.Unification;
import scotch.symbol.type.VariableType;
import scotch.symbol.util.SymbolGenerator;

public abstract class BlockScope extends Scope {

    protected final TypeScope                      types;
    protected final String                         moduleName;
    protected final Map<Symbol, SymbolEntry>       entries;
    protected final Set<Symbol>                    dependencies;
    protected final Map<Symbol, List<PatternCase>> patternCases;
    protected final SymbolResolver                 resolver;
    protected final SymbolGenerator                symbolGenerator;
    protected       Scope                          parent;

    public BlockScope(Scope parent, TypeScope types, String moduleName, SymbolResolver resolver, SymbolGenerator symbolGenerator) {
        this.symbolGenerator = symbolGenerator;
        this.dependencies = new HashSet<>();
        this.entries = new HashMap<>();
        this.types = types;
        this.moduleName = moduleName;
        this.parent = parent;
        this.patternCases = new LinkedHashMap<>();
        this.resolver = resolver;
    }

    @Override
    public void addDependency(Symbol symbol) {
        if (!isExternal(symbol)) {
            dependencies.add(symbol);
        }
    }

    @Override
    public void addPattern(Symbol symbol, PatternCase pattern) {
        patternCases.computeIfAbsent(symbol, k -> new ArrayList<>()).add(pattern);
    }

    @Override
    public Unification bind(VariableType variableType, Type target) {
        return types.bind(variableType, target);
    }

    @Override
    public void defineDataConstructor(Symbol symbol, DataConstructorDescriptor descriptor) {
        define(symbol).defineDataConstructor(descriptor);
    }

    @Override
    public void defineDataType(Symbol symbol, DataTypeDescriptor descriptor) {
        define(symbol).defineDataType(descriptor);
    }

    @Override
    public void defineOperator(Symbol symbol, Operator operator) {
        define(symbol).defineOperator(operator);
    }

    @Override
    public void defineSignature(Symbol symbol, Type type) {
        define(symbol).defineSignature(type);
    }

    @Override
    public void defineValue(Symbol symbol, Type type) {
        define(symbol).defineValue(type, computeValueMethod(symbol, type));
    }

    @Override
    public void extendContext(Type type, Set<Symbol> additionalContext) {
        types.extendContext(type, additionalContext);
    }

    @Override
    public void generalize(Type type) {
        types.generalize(type);
    }

    @Override
    public Type generate(Type type) {
        return types.generate(type);
    }

    @Override
    public Set<Symbol> getDependencies() {
        return new HashSet<>(dependencies);
    }

    @Override
    public Scope getParent() {
        return parent;
    }

    @Override
    public Map<Symbol, List<PatternCase>> getPatternCases() {
        return patternCases;
    }

    @Override
    public Optional<Type> getSignature(Symbol symbol) {
        return getEntry(symbol).flatMap(SymbolEntry::getSignature).map(signature -> signature.genericCopy(types));
    }

    @Override
    public Type getTarget(Type type) {
        return types.getTarget(type);
    }

    @Override
    public Optional<TypeClassDescriptor> getTypeClass(ClassReference classRef) {
        return resolver.getEntry(classRef.getSymbol()).flatMap(SymbolEntry::getTypeClass);
    }

    @Override
    public Set<TypeInstanceDescriptor> getTypeInstances(Symbol typeClass, List<Type> parameters) {
        return resolver.getTypeInstances(typeClass, parameters);
    }

    @Override
    public Optional<MethodSignature> getValueSignature(Symbol symbol) {
        return Optional.ofNullable(entries.get(symbol))
            .map(SymbolEntry::getValueMethod)
            .orElseGet(() -> parent.getValueSignature(symbol));
    }

    @Override
    public void implement(Symbol typeClass, SumType type) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public boolean isBound(VariableType type) {
        return types.isBound(type);
    }

    @Override
    public boolean isGeneric(VariableType variableType) {
        return types.isGeneric(variableType);
    }

    @Override
    public boolean isImplemented(Symbol typeClass, SumType type) {
        return types.isImplemented(typeClass, type);
    }

    @Override
    public boolean isOperator_(Symbol symbol) {
        return getEntry(symbol).map(SymbolEntry::isOperator).orElse(false);
    }

    @Override
    public Scope leaveScope() {
        return parent;
    }

    @Override
    public Symbol qualifyCurrent(Symbol symbol) {
        return symbol.qualifyWith(moduleName);
    }

    @Override
    public Symbol reserveSymbol() {
        return symbolGenerator.reserveSymbol().qualifyWith(moduleName);
    }

    @Override
    public Symbol reserveSymbol(List<String> nestings) {
        return symbolGenerator.reserveSymbol(nestings).qualifyWith(moduleName);
    }

    @Override
    public void specialize(Type type) {
        types.specialize(type);
    }

    protected abstract SymbolEntry define(Symbol symbol);

    @Override
    protected String getModuleName() {
        return moduleName;
    }

    @Override
    protected boolean isDefinedLocally(Symbol symbol) {
        return entries.containsKey(symbol);
    }
}
