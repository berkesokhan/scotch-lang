package scotch.compiler.syntax.scope;

import static scotch.symbol.SymbolEntry.mutableEntry;
import static scotch.util.StringUtil.quote;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

public class ChildScope extends BlockScope {

    private final Set<ChildScope> children;
    private final List<String>    captures;
    private final List<String>    locals;

    ChildScope(Scope parent, TypeScope types, SymbolResolver resolver, SymbolGenerator symbolGenerator, String moduleName) {
        super(parent, types, moduleName, resolver, symbolGenerator);
        this.children = new HashSet<>();
        this.captures = new ArrayList<>();
        this.locals = new ArrayList<>();
    }

    @Override
    public void addLocal(String argument) {
        if (captures.contains(argument)) {
            throw new IllegalStateException("Argument " + quote(argument) + " is a capture!");
        } else if (!locals.contains(argument)) {
            locals.add(argument);
        }
    }

    @Override
    public void capture(String argument) {
        if (!locals.contains(argument) && !captures.contains(argument)) {
            captures.add(argument);
        }
    }

    @Override
    public Scope enterScope() {
        ChildScope child = scope(this, types, resolver, symbolGenerator, moduleName);
        children.add(child);
        return child;
    }

    @Override
    public Scope enterScope(String moduleName, List<Import> imports) {
        throw new IllegalStateException();
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
    public void insertChild(Scope newChild) {
        insertChild_((ChildScope) newChild);
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
    public void setParent(Scope newParent) {
        setParent_((ChildScope) newParent);
    }

    protected SymbolEntry define(Symbol symbol) {
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
    protected boolean isDataConstructor(Symbol symbol) {
        return getEntry(symbol)
            .map(SymbolEntry::isDataConstructor)
            .orElseGet(() -> parent.isDataConstructor(symbol));
    }
}
