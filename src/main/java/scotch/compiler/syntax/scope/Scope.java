package scotch.compiler.syntax.scope;

import static me.qmx.jitescript.util.CodegenUtils.sig;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import scotch.compiler.symbol.DataConstructorDescriptor;
import scotch.compiler.symbol.DataTypeDescriptor;
import scotch.compiler.symbol.MethodSignature;
import scotch.compiler.symbol.Operator;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.SymbolEntry;
import scotch.compiler.symbol.SymbolGenerator;
import scotch.compiler.symbol.SymbolResolver;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.symbol.type.FunctionType;
import scotch.compiler.symbol.TypeClassDescriptor;
import scotch.compiler.symbol.TypeInstanceDescriptor;
import scotch.compiler.symbol.TypeScope;
import scotch.compiler.symbol.exception.SymbolNotFoundException;
import scotch.compiler.syntax.definition.Import;
import scotch.compiler.syntax.reference.ClassReference;
import scotch.compiler.syntax.reference.ModuleReference;
import scotch.compiler.syntax.reference.ValueReference;
import scotch.compiler.syntax.value.PatternMatcher;
import scotch.runtime.Applicable;
import scotch.runtime.Callable;

public abstract class Scope implements TypeScope {

    public static RootScope scope(SymbolGenerator symbolGenerator, SymbolResolver resolver) {
        return new RootScope(symbolGenerator, resolver);
    }

    public static ModuleScope scope(Scope parent, TypeScope types, SymbolResolver resolver, String moduleName, List<Import> imports) {
        return new ModuleScope(parent, types, resolver, moduleName, imports);
    }

    public static ChildScope scope(String moduleName, Scope parent, TypeScope types) {
        return new ChildScope(moduleName, parent, types);
    }

    protected static boolean isConstructor_(Collection<SymbolEntry> entries, Symbol symbol) {
        return entries.stream()
            .filter(entry -> entry.getConstructor(symbol).isPresent())
            .findFirst()
            .isPresent();
    }

    Scope() {
        // intentionally empty
    }

    public abstract void addDependency(Symbol symbol);

    public void addLocal(String argument) {
        throw new IllegalStateException();
    }

    public abstract void addPattern(Symbol symbol, PatternMatcher pattern);

    public void bind(Scope scope) {
        throw new IllegalStateException();
    }

    public void capture(String argument) {
        throw new IllegalStateException();
    }

    public abstract void defineDataType(Symbol symbol, DataTypeDescriptor descriptor);

    public abstract void defineDataConstructor(Symbol symbol, DataConstructorDescriptor descriptor);

    public abstract void defineOperator(Symbol symbol, Operator operator);

    public abstract void defineSignature(Symbol symbol, Type type);

    public abstract void defineValue(Symbol symbol, Type type);

    public abstract Scope enterScope();

    public abstract Scope enterScope(String moduleName, List<Import> imports);

    public List<String> getCaptures() {
        throw new IllegalStateException();
    }

    public String getDataConstructorClass(Symbol symbol) {
        return getEntry(symbol)
            .map(SymbolEntry::getDataConstructor)
            .map(constructor -> constructor.getSymbol().getClassNameAsChildOf(constructor.getDataType()))
            .orElseThrow(() -> new SymbolNotFoundException("Can't get data constructor class for " + symbol.quote()));
    }

    public abstract Set<Symbol> getDependencies();

    public List<String> getLocals() {
        throw new IllegalStateException();
    }

    public abstract TypeClassDescriptor getMemberOf(ValueReference valueRef);

    public abstract Operator getOperator(Symbol symbol);

    public abstract Scope getParent();

    public abstract Map<Symbol, List<PatternMatcher>> getPatterns();

    public Type getRawValue(ValueReference reference) {
        return getRawValue(reference.getSymbol());
    }

    public abstract Type getRawValue(Symbol symbol);

    public abstract Optional<Type> getSignature(Symbol symbol);

    public abstract TypeClassDescriptor getTypeClass(ClassReference classRef);

    public TypeInstanceDescriptor getTypeInstance(ClassReference classReference, ModuleReference moduleReference, List<Type> types) {
        return getTypeInstances(classReference.getSymbol(), types).stream()
            .filter(instance -> moduleReference.is(instance.getModuleName()))
            .findFirst()
            .orElseThrow(UnsupportedOperationException::new);
    }

    public abstract Set<TypeInstanceDescriptor> getTypeInstances(Symbol typeClass, List<Type> parameters);

    public Type getValue(ValueReference reference) {
        return getValue(reference.getSymbol());
    }

    public Type getValue(Symbol symbol) {
        return genericCopy(getRawValue(symbol));
    }

    public abstract Optional<MethodSignature> getValueSignature(Symbol symbol);

    public void insert(Scope scope) {
        throw new IllegalStateException();
    }

    public abstract boolean isDefined(Symbol symbol);

    public boolean isMember(Symbol symbol) {
        return getEntry(symbol).map(SymbolEntry::isMember).orElse(false);
    }

    public boolean isOperator(Symbol symbol) {
        return qualify(symbol).map(this::isOperator_).orElse(false);
    }

    public abstract Scope leaveScope();

    public void prependLocals(List<String> locals) {
        throw new IllegalStateException();
    }

    public abstract Optional<Symbol> qualify(Symbol symbol);

    public abstract Symbol qualifyCurrent(Symbol symbol);

    public void redefineSignature(Symbol symbol, Type type) {
        Optional<SymbolEntry> optionalEntry = getEntry(symbol);
        if (optionalEntry.isPresent()) {
            optionalEntry.get().redefineSignature(type);
        } else {
            throw new SymbolNotFoundException("Can't redefine non-existent value " + symbol.quote());
        }
    }

    public void redefineValue(Symbol symbol, Type type) {
        Optional<SymbolEntry> optionalEntry = getEntry(symbol);
        if (optionalEntry.isPresent()) {
            optionalEntry.get().redefineValue(type, computeValueMethod(symbol, type));
        } else {
            throw new SymbolNotFoundException("Can't redefine non-existent value " + symbol.quote());
        }
    }

    public abstract Symbol reserveSymbol();

    public Symbol reserveSymbol(List<String> nestings) {
        return reserveSymbol().nest(nestings);
    }

    public Type reserveType() {
        return getParent().reserveType();
    }

    protected MethodSignature computeValueMethod(Symbol symbol, Type type) {
        return MethodSignature.staticMethod(
            symbol.qualifyWith(getModuleName()).getModuleClass(),
            symbol.getMethodName(),
            type instanceof FunctionType ? sig(Applicable.class) : sig(Callable.class)
        );
    }

    protected abstract Optional<SymbolEntry> getEntry(Symbol symbol);

    protected abstract String getModuleName();

    protected abstract boolean isDataConstructor(Symbol symbol);

    protected abstract boolean isDefinedLocally(Symbol symbol);

    protected boolean isExternal(Symbol symbol) {
        return getParent().isExternal(symbol);
    }

    protected abstract boolean isOperator_(Symbol symbol);
}
