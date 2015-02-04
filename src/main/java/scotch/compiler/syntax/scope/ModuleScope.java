package scotch.compiler.syntax.scope;

import static java.util.stream.Collectors.toSet;
import static scotch.compiler.symbol.SymbolEntry.mutableEntry;
import static scotch.compiler.syntax.reference.DefinitionReference.classRef;
import static scotch.util.StringUtil.quote;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import scotch.compiler.symbol.DataConstructorDescriptor;
import scotch.compiler.symbol.DataTypeDescriptor;
import scotch.compiler.symbol.MethodSignature;
import scotch.compiler.symbol.Operator;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Symbol.QualifiedSymbol;
import scotch.compiler.symbol.Symbol.SymbolVisitor;
import scotch.compiler.symbol.Symbol.UnqualifiedSymbol;
import scotch.compiler.symbol.SymbolEntry;
import scotch.compiler.symbol.SymbolResolver;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.symbol.type.VariableType;
import scotch.compiler.symbol.TypeClassDescriptor;
import scotch.compiler.symbol.TypeInstanceDescriptor;
import scotch.compiler.symbol.TypeScope;
import scotch.compiler.symbol.Unification;
import scotch.compiler.symbol.exception.SymbolNotFoundException;
import scotch.compiler.syntax.definition.Import;
import scotch.compiler.syntax.reference.ClassReference;
import scotch.compiler.syntax.reference.ValueReference;
import scotch.compiler.syntax.value.PatternMatcher;

public class ModuleScope extends Scope {

    private final Scope                             parent;
    private final TypeScope                         types;
    private final SymbolResolver                    resolver;
    private final String                            moduleName;
    private final List<Import>                      imports;
    private final Map<Symbol, SymbolEntry>          entries;
    private final Map<Symbol, List<PatternMatcher>> patterns;
    private final Set<Symbol>                       dependencies;

    ModuleScope(Scope parent, TypeScope types, SymbolResolver resolver, String moduleName, List<Import> imports) {
        this.parent = parent;
        this.types = types;
        this.resolver = resolver;
        this.moduleName = moduleName;
        this.imports = ImmutableList.copyOf(imports);
        this.entries = new HashMap<>();
        this.patterns = new LinkedHashMap<>();
        this.dependencies = new HashSet<>();
    }

    @Override
    public void addDependency(Symbol symbol) {
        if (!isExternal(symbol)) {
            dependencies.add(symbol);
        }
    }

    @Override
    public void addPattern(Symbol symbol, PatternMatcher pattern) {
        patterns.computeIfAbsent(symbol, k -> new ArrayList<>()).add(pattern);
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
    public Scope enterScope() {
        return scope(moduleName, this, types);
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
    public Set<Symbol> getContext(Type type) {
        return ImmutableSet.<Symbol>builder()
            .addAll(types.getContext(type))
            .addAll(imports.stream()
                .map(import_ -> import_.getContext(type, resolver))
                .flatMap(Collection::stream)
                .collect(toSet()))
            .build();
    }

    @Override
    public Set<Symbol> getDependencies() {
        return new HashSet<>(dependencies);
    }

    @Override
    public TypeClassDescriptor getMemberOf(ValueReference valueRef) {
        return resolver.getEntry(valueRef.getSymbol())
            .map(SymbolEntry::getMemberOf)
            .map(symbol -> getTypeClass(classRef(symbol)))
            .orElseThrow(() -> new SymbolNotFoundException("Could not get parent class of symbol " + valueRef.getSymbol().quote()));
    }

    @Override
    public Operator getOperator(Symbol symbol) {
        return getEntry(symbol)
            .map(SymbolEntry::getOperator)
            .orElseThrow(() -> new SymbolNotFoundException("Symbol " + symbol.quote() + " is not an operator"));
    }

    @Override
    public Scope getParent() {
        return parent;
    }

    @Override
    public Map<Symbol, List<PatternMatcher>> getPatterns() {
        return patterns;
    }

    @Override
    public Type getRawValue(Symbol symbol) {
        return getEntry(symbol)
            .map(SymbolEntry::getValue)
            .orElseThrow(() -> new SymbolNotFoundException("Symbol " + symbol.quote() + " is not a value"));
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
    public TypeClassDescriptor getTypeClass(ClassReference classRef) {
        return resolver.getEntry(classRef.getSymbol())
            .map(SymbolEntry::getTypeClass)
            .orElseThrow(() -> new SymbolNotFoundException("Symbol " + classRef.getSymbol().quote() + " is not a type class"));
    }

    @Override
    public Set<TypeInstanceDescriptor> getTypeInstances(Symbol typeClass, List<Type> parameters) {
        return resolver.getTypeInstances(typeClass, parameters);
    }

    @Override
    public Optional<MethodSignature> getValueSignature(Symbol symbol) {
        if (entries.containsKey(symbol)) {
            return Optional.of(entries.get(symbol).getValueMethod());
        } else {
            return parent.getValueSignature(symbol);
        }
    }

    @Override
    public boolean isBound(VariableType type) {
        return types.isBound(type);
    }

    @Override
    public boolean isDefined(Symbol symbol) {
        return getEntry(symbol).isPresent();
    }

    @Override
    public boolean isGeneric(VariableType variableType) {
        return types.isGeneric(variableType);
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
    public Optional<Symbol> qualify(Symbol symbol) {
        return symbol.accept(new SymbolVisitor<Optional<Symbol>>() {
            @Override
            public Optional<Symbol> visit(QualifiedSymbol symbol) {
                if (Objects.equals(moduleName, symbol.getModuleName())) {
                    return Optional.of(symbol);
                } else {
                    return imports.stream()
                        .filter(i -> i.isFrom(symbol.getModuleName()))
                        .findFirst()
                        .flatMap(i -> i.qualify(symbol.getSimpleName(), resolver));
                }
            }

            @Override
            public Optional<Symbol> visit(UnqualifiedSymbol symbol) {
                Symbol qualified = symbol.qualifyWith(moduleName);
                if (isDefinedLocally(qualified)) {
                    return Optional.of(qualified);
                } else {
                    return imports.stream()
                        .map(i -> i.qualify(symbol.getSimpleName(), resolver))
                        .filter(Optional::isPresent)
                        .findFirst()
                        .flatMap(s -> s);
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
        return parent.reserveSymbol().qualifyWith(moduleName);
    }

    @Override
    public Symbol reserveSymbol(List<String> nestings) {
        return getParent().reserveSymbol(nestings).qualifyWith(moduleName);
    }

    @Override
    public void specialize(Type type) {
        types.specialize(type);
    }

    private SymbolEntry define(Symbol symbol) {
        return symbol.accept(new SymbolVisitor<SymbolEntry>() {
            @Override
            public SymbolEntry visit(QualifiedSymbol symbol) {
                if (Objects.equals(moduleName, symbol.getModuleName())) {
                    return entries.computeIfAbsent(symbol, k -> mutableEntry(symbol));
                } else {
                    throw new IllegalArgumentException("Can't define symbol " + symbol.quote() + " within different module " + quote(moduleName));
                }
            }

            @Override
            public SymbolEntry visit(UnqualifiedSymbol symbol) {
                throw new IllegalArgumentException("Can't define unqualified symbol " + symbol.quote());
            }
        });
    }

    @Override
    protected Optional<SymbolEntry> getEntry(Symbol symbol) {
        return symbol.accept(new SymbolVisitor<Optional<SymbolEntry>>() {
            @Override
            public Optional<SymbolEntry> visit(QualifiedSymbol symbol) {
                Optional<SymbolEntry> optionalEntry = Optional.ofNullable(entries.get(symbol));
                if (optionalEntry.isPresent()) {
                    return optionalEntry;
                } else {
                    return parent.getEntry(symbol);
                }
            }

            @Override
            public Optional<SymbolEntry> visit(UnqualifiedSymbol symbol) {
                return qualify(symbol).flatMap(ModuleScope.this::getEntry);
            }
        });
    }

    @Override
    protected String getModuleName() {
        return moduleName;
    }

    @Override
    protected boolean isDataConstructor(Symbol symbol) {
        return isConstructor_(entries.values(), symbol) || parent.isDataConstructor(symbol);
    }

    @Override
    protected boolean isDefinedLocally(Symbol symbol) {
        return entries.containsKey(symbol);
    }
}
