package scotch.compiler.syntax.scope;

import static scotch.symbol.SymbolEntry.mutableEntry;
import static scotch.util.StringUtil.quote;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import scotch.symbol.MethodSignature;
import scotch.symbol.Operator;
import scotch.symbol.Symbol;
import scotch.symbol.Symbol.QualifiedSymbol;
import scotch.symbol.Symbol.SymbolVisitor;
import scotch.symbol.Symbol.UnqualifiedSymbol;
import scotch.symbol.SymbolEntry;
import scotch.symbol.descriptor.DataConstructorDescriptor;
import scotch.symbol.descriptor.DataTypeDescriptor;
import scotch.symbol.descriptor.TypeClassDescriptor;
import scotch.symbol.descriptor.TypeInstanceDescriptor;
import scotch.symbol.type.SumType;
import scotch.symbol.type.Type;
import scotch.symbol.type.TypeScope;
import scotch.symbol.type.Unification;
import scotch.symbol.type.VariableType;
import scotch.compiler.syntax.definition.Import;
import scotch.compiler.syntax.reference.ClassReference;
import scotch.compiler.syntax.reference.ValueReference;
import scotch.compiler.syntax.pattern.PatternCase;

public class ChildScope extends Scope {

    private final TypeScope                      types;
    private final Set<ChildScope>                children;
    private final Map<Symbol, SymbolEntry>       entries;
    private final Map<Symbol, List<PatternCase>> patternCases;
    private final Set<Symbol>                    dependencies;
    private final List<String>                   captures;
    private final List<String>                   locals;
    private final String                         moduleName;
    private       Scope                          parent;

    ChildScope(String moduleName, Scope parent, TypeScope types) {
        this.moduleName = moduleName;
        this.parent = parent;
        this.types = types;
        this.children = new HashSet<>();
        this.entries = new HashMap<>();
        this.patternCases = new LinkedHashMap<>();
        this.dependencies = new HashSet<>();
        this.captures = new ArrayList<>();
        this.locals = new ArrayList<>();
    }

    @Override
    public void addDependency(Symbol symbol) {
        if (!isExternal(symbol)) {
            dependencies.add(symbol);
        }
    }

    @Override
    public void addLocal(String argument) {
        if (captures.contains(argument)) {
            throw new IllegalStateException("Argument " + quote(argument) + " is a capture!");
        } else if (!locals.contains(argument)) {
            locals.add(argument);
        }
    }

    public void addPattern(Symbol symbol, PatternCase pattern) {
        patternCases.computeIfAbsent(symbol, k -> new ArrayList<>()).add(pattern);
    }

    @Override
    public Unification bind(VariableType variableType, Type target) {
        return types.bind(variableType, target);
    }

    @Override
    public void capture(String argument) {
        if (!locals.contains(argument) && !captures.contains(argument)) {
            captures.add(argument);
        }
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
        throw new IllegalStateException("Can't define operator " + symbol.quote() + " in this scope");
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
    public Scope enterScope() {
        ChildScope child = scope(moduleName, this, types);
        children.add(child);
        return child;
    }

    @Override
    public Scope enterScope(String moduleName, List<Import> imports) {
        throw new IllegalStateException();
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
    public List<String> getCaptures() {
        return ImmutableList.copyOf(captures);
    }

    @Override
    public Set<Symbol> getContext(Type type) {
        return ImmutableSet.<Symbol>builder()
            .addAll(types.getContext(type))
            .addAll(parent.getContext(type))
            .build();
    }

    @Override
    public Set<Symbol> getDependencies() {
        return new HashSet<>(dependencies);
    }

    @Override
    public List<String> getLocals() {
        return ImmutableList.copyOf(locals);
    }

    @Override
    public Optional<TypeClassDescriptor> getMemberOf(ValueReference valueRef) {
        return parent.getMemberOf(valueRef);
    }

    @Override
    public Optional<Operator> getOperator(Symbol symbol) {
        return getEntry(symbol).flatMap(entry -> {
            Optional<Operator> operator = entry.getOperator();
            if (operator.isPresent()) {
                return operator;
            } else {
                return parent.getOperator(symbol);
            }
        });
    }

    @Override
    public Scope getParent() {
        return parent;
    }

    public Map<Symbol, List<PatternCase>> getPatternCases() {
        return patternCases;
    }

    @Override
    public Optional<Type> getRawValue(Symbol symbol) {
        return getEntry(symbol).flatMap(entry -> {
            Optional<Type> rawValue = entry.getValue();
            if (rawValue.isPresent()) {
                return rawValue;
            } else {
                return parent.getRawValue(symbol);
            }
        });
    }

    @Override
    public Optional<Type> getSignature(Symbol symbol) {
        return getEntry(symbol)
            .flatMap(entry -> entry.getSignature()
                .map(signature -> signature.genericCopy(types)));
    }

    @Override
    public Type getTarget(Type type) {
        return types.getTarget(type);
    }

    @Override
    public void implement(Symbol typeClass, SumType type) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public Optional<TypeClassDescriptor> getTypeClass(ClassReference classRef) {
        return parent.getTypeClass(classRef);
    }

    @Override
    public Set<TypeInstanceDescriptor> getTypeInstances(Symbol typeClass, List<Type> parameters) {
        return parent.getTypeInstances(typeClass, parameters);
    }

    @Override
    public Optional<MethodSignature> getValueSignature(Symbol symbol) {
        return Optional.ofNullable(entries.get(symbol))
            .map(SymbolEntry::getValueMethod)
            .orElseGet(() -> parent.getValueSignature(symbol));
    }

    @Override
    public void insertChild(Scope newChild) {
        insertChild_((ChildScope) newChild);
    }

    @Override
    public boolean isBound(VariableType type) {
        return types.isBound(type);
    }

    @Override
    public boolean isDefined(Symbol symbol) {
        return symbol.accept(new SymbolVisitor<Boolean>() {
            @Override
            public Boolean visit(QualifiedSymbol symbol) {
                return parent.isDefined(symbol);
            }

            @Override
            public Boolean visit(UnqualifiedSymbol symbol) {
                return isDefinedLocally(symbol) || parent.isDefined(symbol);
            }
        });
    }

    @Override
    public boolean isGeneric(VariableType variableType) {
        return parent.isGeneric(variableType);
    }

    @Override
    public boolean isImplemented(Symbol typeClass, SumType type) {
        return parent.isImplemented(typeClass, type);
    }

    @Override
    public boolean isOperator_(Symbol symbol) {
        return parent.isOperator_(symbol);
    }

    @Override
    public Scope leaveScope() {
        return parent;
    }

    @Override
    public void prependLocals(List<String> locals) {
        this.locals.addAll(0, locals);
    }

    @Override
    public Optional<Symbol> qualify(Symbol symbol) {
        return symbol.accept(new SymbolVisitor<Optional<Symbol>>() {
            @Override
            public Optional<Symbol> visit(QualifiedSymbol symbol) {
                if (moduleName.equals(symbol.getModuleName()) && entries.containsKey(symbol)) {
                    return Optional.of(symbol);
                } else {
                    return parent.qualify(symbol);
                }
            }

            @Override
            public Optional<Symbol> visit(UnqualifiedSymbol symbol) {
                if (entries.containsKey(symbol.qualifyWith(moduleName))) {
                    return Optional.of(symbol.qualifyWith(moduleName));
                } else if (entries.containsKey(symbol)) {
                    return Optional.of(symbol);
                } else {
                    return parent.qualify(symbol);
                }
            }
        });
    }

    @Override
    public Symbol qualifyCurrent(Symbol symbol) {
        return symbol.qualifyWith(moduleName);
    }

    @Override
    public Symbol reserveSymbol() {
        return parent.reserveSymbol();
    }

    @Override
    public Symbol reserveSymbol(List<String> nestings) {
        return parent.reserveSymbol(nestings);
    }

    @Override
    public void setParent(Scope newParent) {
        setParent_((ChildScope) newParent);
    }

    @Override
    public void specialize(Type type) {
        types.specialize(type);
    }

    private SymbolEntry define(Symbol symbol) {
        return entries.computeIfAbsent(symbol, k -> mutableEntry(symbol));
    }

    private void insertChild_(ChildScope newChild) {
        children.forEach(child -> child.setParent(newChild));
        children.clear();
        newChild.setParent(this);
    }

    private void setParent_(ChildScope newParent) {
        newParent.children.add(this);
        parent = newParent;
    }

    @Override
    protected Optional<SymbolEntry> getEntry(Symbol symbol) {
        Optional<SymbolEntry> localEntry = Optional.ofNullable(entries.get(symbol));
        if (localEntry.isPresent()) {
            return localEntry;
        } else {
            return parent.getEntry(symbol);
        }
    }

    @Override
    protected String getModuleName() {
        return moduleName;
    }

    @Override
    protected boolean isDataConstructor(Symbol symbol) {
        return getEntry(symbol)
            .map(SymbolEntry::isDataConstructor)
            .orElseGet(() -> parent.isDataConstructor(symbol));
    }

    @Override
    protected boolean isDefinedLocally(Symbol symbol) {
        return entries.containsKey(symbol);
    }
}
