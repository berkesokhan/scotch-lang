package scotch.compiler.syntax.scope;

import static java.util.stream.Collectors.toSet;
import static scotch.compiler.syntax.reference.DefinitionReference.classRef;
import static scotch.symbol.SymbolEntry.mutableEntry;
import static scotch.util.StringUtil.quote;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import scotch.compiler.syntax.definition.Import;
import scotch.compiler.syntax.reference.ValueReference;
import scotch.symbol.Operator;
import scotch.symbol.Symbol;
import scotch.symbol.Symbol.QualifiedSymbol;
import scotch.symbol.Symbol.SymbolVisitor;
import scotch.symbol.Symbol.UnqualifiedSymbol;
import scotch.symbol.SymbolEntry;
import scotch.symbol.SymbolResolver;
import scotch.symbol.descriptor.TypeClassDescriptor;
import scotch.symbol.type.Type;
import scotch.symbol.type.TypeScope;
import scotch.symbol.util.SymbolGenerator;

public class ModuleScope extends BlockScope {

    private final List<Import> imports;

    ModuleScope(Scope parent, TypeScope types, SymbolResolver resolver, SymbolGenerator symbolGenerator, String moduleName, List<Import> imports) {
        super(parent, types, moduleName, resolver, symbolGenerator);
        this.imports = ImmutableList.copyOf(imports);
    }

    @Override
    public Scope enterScope() {
        return scope(this, types, resolver, symbolGenerator, moduleName);
    }

    @Override
    public Scope enterScope(String moduleName, List<Import> imports) {
        throw new IllegalStateException();
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
    public Optional<TypeClassDescriptor> getMemberOf(ValueReference valueRef) {
        return resolver.getEntry(valueRef.getSymbol())
            .flatMap(SymbolEntry::getMemberOf)
            .flatMap(symbol -> getTypeClass(classRef(symbol)));
    }

    @Override
    public Optional<Operator> getOperator(Symbol symbol) {
        return getEntry(symbol).flatMap(SymbolEntry::getOperator);
    }

    @Override
    public Optional<Type> getRawValue(Symbol symbol) {
        return getEntry(symbol).flatMap(SymbolEntry::getValue);
    }

    @Override
    public boolean isDefined(Symbol symbol) {
        return getEntry(symbol).isPresent();
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
    protected SymbolEntry define(Symbol symbol) {
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
    protected boolean isDataConstructor(Symbol symbol) {
        return isConstructor_(entries.values(), symbol) || parent.isDataConstructor(symbol);
    }
}
