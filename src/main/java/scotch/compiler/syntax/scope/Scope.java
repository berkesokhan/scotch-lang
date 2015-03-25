package scotch.compiler.syntax.scope;

import static me.qmx.jitescript.util.CodegenUtils.sig;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import scotch.compiler.syntax.definition.Import;
import scotch.compiler.syntax.pattern.PatternCase;
import scotch.compiler.syntax.reference.ClassReference;
import scotch.compiler.syntax.reference.ModuleReference;
import scotch.compiler.syntax.reference.ValueReference;
import scotch.runtime.Callable;
import scotch.symbol.MethodSignature;
import scotch.symbol.Operator;
import scotch.symbol.Symbol;
import scotch.symbol.SymbolEntry;
import scotch.symbol.SymbolResolver;
import scotch.symbol.descriptor.DataConstructorDescriptor;
import scotch.symbol.descriptor.DataTypeDescriptor;
import scotch.symbol.descriptor.TypeClassDescriptor;
import scotch.symbol.descriptor.TypeInstanceDescriptor;
import scotch.symbol.type.Type;
import scotch.symbol.type.TypeScope;
import scotch.symbol.type.VariableType;
import scotch.symbol.util.SymbolGenerator;

public abstract class Scope implements TypeScope {

    public static RootScope scope(SymbolGenerator symbolGenerator, SymbolResolver resolver) {
        return new RootScope(symbolGenerator, resolver);
    }

    public static ModuleScope scope(Scope parent, TypeScope types, SymbolResolver resolver, SymbolGenerator symbolGenerator, String moduleName, List<Import> imports) {
        return new ModuleScope(parent, types, resolver, symbolGenerator, moduleName, imports);
    }

    public static ChildScope scope(Scope parent, TypeScope types, SymbolResolver resolver, SymbolGenerator symbolGenerator, String moduleName) {
        return new ChildScope(parent, types, resolver, symbolGenerator, moduleName);
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

    public abstract void addPattern(Symbol symbol, PatternCase pattern);

    public void capture(String argument) {
        throw new IllegalStateException();
    }

    public abstract void defineDataConstructor(Symbol symbol, DataConstructorDescriptor descriptor);

    public abstract void defineDataType(Symbol symbol, DataTypeDescriptor descriptor);

    public abstract void defineOperator(Symbol symbol, Operator operator);

    public abstract void defineSignature(Symbol symbol, Type type);

    public abstract void defineValue(Symbol symbol, Type type);

    public abstract Scope enterScope();

    public abstract Scope enterScope(String moduleName, List<Import> imports);

    public List<String> getCaptures() {
        throw new IllegalStateException();
    }

    public Optional<DataConstructorDescriptor> getDataConstructor(Symbol symbol) {
        return getEntry(symbol).flatMap(SymbolEntry::getDataConstructor);
    }

    public String getDataConstructorClass(Symbol symbol) {
        return getEntry(symbol)
            .flatMap(SymbolEntry::getDataConstructor)
            .map(constructor -> constructor.getSymbol().getClassNameAsChildOf(constructor.getDataType()))
            .orElseThrow(() -> new IllegalStateException("Can't get data constructor class for " + symbol.quote()));
    }

    public abstract Set<Symbol> getDependencies();

    public List<String> getLocals() {
        throw new IllegalStateException();
    }

    public abstract Optional<TypeClassDescriptor> getMemberOf(ValueReference valueRef);

    public abstract Optional<Operator> getOperator(Symbol symbol);

    public abstract Scope getParent();

    public abstract Map<Symbol, List<PatternCase>> getPatternCases();

    public Optional<Type> getRawValue(ValueReference reference) {
        return getRawValue(reference.getSymbol());
    }

    public abstract Optional<Type> getRawValue(Symbol symbol);

    public abstract Optional<Type> getSignature(Symbol symbol);

    public abstract Optional<TypeClassDescriptor> getTypeClass(ClassReference classRef);

    public Optional<TypeInstanceDescriptor> getTypeInstance(ClassReference classReference, ModuleReference moduleReference, List<Type> parameters) {
        return getTypeInstances(classReference.getSymbol(), parameters).stream()
            .filter(instance -> moduleReference.is(instance.getModuleName()))
            .findFirst();
    }

    public abstract Set<TypeInstanceDescriptor> getTypeInstances(Symbol typeClass, List<Type> parameters);

    public Optional<Type> getValue(ValueReference reference) {
        return getValue(reference.getSymbol());
    }

    public Optional<Type> getValue(Symbol symbol) {
        return getRawValue(symbol).map(type -> type.genericCopy(this));
    }

    public abstract Optional<MethodSignature> getValueSignature(Symbol symbol);

    public void insertChild(Scope scope) {
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

    public void redefineDataConstructor(Symbol symbol, DataConstructorDescriptor descriptor) {
        Optional<SymbolEntry> optionalEntry = getEntry(symbol);
        if (optionalEntry.isPresent()) {
            optionalEntry.get().redefineDataConstructor(descriptor);
        } else {
            throw new IllegalStateException("Can't redefine non-existent data constructor " + symbol.quote());
        }
    }

    public void redefineDataType(Symbol symbol, DataTypeDescriptor descriptor) {
        Optional<SymbolEntry> optionalEntry = getEntry(symbol);
        if (optionalEntry.isPresent()) {
            optionalEntry.get().redefineDataType(descriptor);
        } else {
            throw new IllegalStateException("Can't redefine non-existent data constructor " + symbol.quote());
        }
    }

    public void redefineSignature(Symbol symbol, Type type) {
        Optional<SymbolEntry> optionalEntry = getEntry(symbol);
        if (optionalEntry.isPresent()) {
            optionalEntry.get().redefineSignature(type);
        } else {
            throw new IllegalStateException("Can't redefine non-existent value " + symbol.quote());
        }
    }

    public void redefineValue(Symbol symbol, Type type) {
        Optional<SymbolEntry> optionalEntry = getEntry(symbol);
        if (optionalEntry.isPresent()) {
            optionalEntry.get().redefineValue(type, computeValueMethod(symbol, type));
        } else {
            throw new IllegalStateException("Can't redefine non-existent value " + symbol.quote());
        }
    }

    public abstract Symbol reserveSymbol();

    public Symbol reserveSymbol(List<String> nestings) {
        return reserveSymbol().nest(nestings);
    }

    public VariableType reserveType() {
        return getParent().reserveType();
    }

    public void setParent(Scope scope) {
        throw new IllegalStateException();
    }

    protected MethodSignature computeValueMethod(Symbol symbol, Type type) {
        return MethodSignature.staticMethod(
            symbol.qualifyWith(getModuleName()).getModuleClass(),
            symbol.getMethodName(),
            sig(Callable.class)
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
