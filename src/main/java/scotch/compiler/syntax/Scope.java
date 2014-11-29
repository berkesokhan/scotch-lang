package scotch.compiler.syntax;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import com.google.common.collect.ImmutableList;
import scotch.compiler.syntax.Symbol.QualifiedSymbol;
import scotch.compiler.syntax.Symbol.SymbolVisitor;
import scotch.compiler.syntax.Symbol.UnqualifiedSymbol;
import scotch.compiler.syntax.Type.VariableType;

public abstract class Scope implements TypeScope {

    public static Scope scope(TypeGenerator typeGenerator, SymbolResolver resolver) {
        return new RootScope(typeGenerator, resolver);
    }

    public static Scope scope(Scope parent, SymbolResolver resolver, String moduleName, List<Import> imports) {
        return new ModuleScope(parent, new DefaultTypeScope(), resolver, moduleName, imports);
    }

    public static Scope scope(Scope parent) {
        return new ChildScope(parent, new DefaultTypeScope());
    }

    public static SymbolNotFoundException symbolNotFound(Symbol symbol) {
        return new SymbolNotFoundException("Could not find symbol " + symbol.quote());
    }

    private Scope() {
        // intentionally empty
    }

    public abstract void addPattern(Symbol symbol);

    public abstract void defineOperator(Symbol symbol, Operator operator);

    public abstract void defineSignature(Symbol symbol, Type type);

    public abstract void defineValue(Symbol symbol, Type type);

    public abstract Scope enterScope();

    public abstract Scope enterScope(String moduleName, List<Import> imports);

    @Override
    public Set<Symbol> getContext(Type type) {
        throw new UnsupportedOperationException(); // TODO
    }

    public abstract Operator getOperator(Symbol symbol);

    public abstract Optional<Type> getSignature(Symbol symbol);

    public abstract Type getValue(Symbol symbol);

    public abstract boolean isDefined(Symbol symbol);

    public boolean isOperator(Symbol symbol) {
        return qualify(symbol).map(this::isOperator_).orElse(false);
    }

    public abstract boolean isPattern(Symbol symbol);

    public abstract Scope leaveScope();

    public abstract Optional<Symbol> qualify(Symbol symbol);

    public abstract Symbol qualifyCurrent(Symbol symbol);

    public abstract void redefineValue(Symbol symbol, Type type);

    public abstract Type reserveType();

    protected abstract Optional<SymbolEntry> getEntry(Symbol symbol);

    protected abstract boolean isDefinedLocally(Symbol symbol);

    protected abstract boolean isOperator_(Symbol symbol);

    public static class ChildScope extends Scope {

        private final Scope                    parent;
        private final TypeScope                types;
        private final Map<Symbol, SymbolEntry> entries;
        private final Set<Symbol>              patterns;

        private ChildScope(Scope parent, TypeScope types) {
            this.parent = parent;
            this.types = types;
            this.entries = new HashMap<>();
            this.patterns = new HashSet<>();
        }

        @Override
        public void addPattern(Symbol symbol) {
            patterns.add(symbol);
        }

        @Override
        public void bind(VariableType variableType, Type target) {
            types.bind(variableType, target);
        }

        @Override
        public void extendContext(Type type, Set<Symbol> additionalContext) {
            types.extendContext(type, additionalContext);
        }

        @Override
        public void defineOperator(Symbol symbol, Operator operator) {
            throw new IllegalStateException("Can't define operator " + symbol.quote() + " in this scope"); // TODO
        }

        @Override
        public void defineSignature(Symbol symbol, Type type) {
            define(symbol).defineSignature(type);
        }

        @Override
        public void defineValue(Symbol symbol, Type type) {
            define(symbol).defineValue(type);
        }

        @Override
        public Scope enterScope() {
            return scope(this);
        }

        @Override
        public Scope enterScope(String moduleName, List<Import> imports) {
            throw new IllegalStateException();
        }

        @Override
        public Type generate(Type type) {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public Set<Symbol> getContext(Type type) {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public Operator getOperator(Symbol symbol) {
            return requireEntry(symbol).getOperator();
        }

        @Override
        public Optional<Type> getSignature(Symbol symbol) {
            return requireEntry(symbol).getSignature();
        }

        @Override
        public Type getTarget(Type type) {
            return types.getTarget(type);
        }

        @Override
        public Type getValue(Symbol symbol) {
            return requireEntry(symbol).getValue();
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
        public boolean isOperator_(Symbol symbol) {
            return parent.isOperator_(symbol);
        }

        @Override
        public boolean isPattern(Symbol symbol) {
            return patterns.contains(symbol);
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
                    return parent.qualify(symbol);
                }

                @Override
                public Optional<Symbol> visit(UnqualifiedSymbol symbol) {
                    if (isDefinedLocally(symbol)) {
                        return Optional.of(symbol);
                    } else {
                        return parent.qualify(symbol);
                    }
                }
            });
        }

        @Override
        public Symbol qualifyCurrent(Symbol symbol) {
            return parent.qualifyCurrent(symbol);
        }

        @Override
        public void redefineValue(Symbol symbol, Type type) {
            Optional<SymbolEntry> optionalEntry = getEntry(symbol);
            if (optionalEntry.isPresent()) {
                optionalEntry.get().redefineValue(type);
            } else {
                throw symbolNotFound(symbol);
            }
        }

        @Override
        public Type reserveType() {
            return parent.reserveType();
        }

        private SymbolEntry define(Symbol symbol) {
            return symbol.accept(new SymbolVisitor<SymbolEntry>() {
                @Override
                public SymbolEntry visit(QualifiedSymbol symbol) {
                    throw new IllegalStateException("Can't define symbol with qualified name " + symbol.quote());
                }

                @Override
                public SymbolEntry visit(UnqualifiedSymbol symbol) {
                    return entries.computeIfAbsent(symbol, SymbolEntry::mutableEntry);
                }
            });
        }

        private SymbolEntry requireEntry(Symbol symbol) {
            return getEntry(symbol).orElseThrow(() -> symbolNotFound(symbol));
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
        protected boolean isDefinedLocally(Symbol symbol) {
            return entries.containsKey(symbol);
        }
    }

    public static class ModuleScope extends Scope {

        private final Scope                    parent;
        private final TypeScope                types;
        private final SymbolResolver           resolver;
        private final String                   moduleName;
        private final List<Import>             imports;
        private final Map<Symbol, SymbolEntry> entries;
        private final Set<Symbol>              patterns;

        private ModuleScope(Scope parent, TypeScope types, SymbolResolver resolver, String moduleName, List<Import> imports) {
            this.parent = parent;
            this.types = types;
            this.resolver = resolver;
            this.moduleName = moduleName;
            this.imports = ImmutableList.copyOf(imports);
            this.entries = new HashMap<>();
            this.patterns = new HashSet<>();
        }

        @Override
        public void addPattern(Symbol symbol) {
            patterns.add(symbol);
        }

        @Override
        public void bind(VariableType variableType, Type target) {
            types.bind(variableType, target);
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
            define(symbol).defineValue(type);
        }

        @Override
        public Scope enterScope() {
            return scope(this);
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
        public Type generate(Type type) {
            return types.generate(type);
        }

        @Override
        public Set<Symbol> getContext(Type type) {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public Operator getOperator(Symbol symbol) {
            return getEntry(symbol).map(SymbolEntry::getOperator).orElseThrow(() -> symbolNotFound(symbol));
        }

        @Override
        public Optional<Type> getSignature(Symbol symbol) {
            return getEntry(symbol).flatMap(SymbolEntry::getSignature);
        }

        @Override
        public Type getTarget(Type type) {
            return types.getTarget(type);
        }

        @Override
        public Type getValue(Symbol symbol) {
            return getEntry(symbol)
                .orElseThrow(() -> symbolNotFound(symbol))
                .getValue();
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
        public boolean isOperator_(Symbol symbol) {
            return getEntry(symbol).map(SymbolEntry::isOperator).orElse(false);
        }

        @Override
        public boolean isPattern(Symbol symbol) {
            return patterns.contains(symbol);
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
                            .flatMap(i -> i.qualify(symbol.getMemberName(), resolver));
                    }
                }

                @Override
                public Optional<Symbol> visit(UnqualifiedSymbol symbol) {
                    Symbol qualified = symbol.qualifyWith(moduleName);
                    if (isDefinedLocally(qualified)) {
                        return Optional.of(qualified);
                    } else {
                        return imports.stream()
                            .map(i -> i.qualify(symbol.getMemberName(), resolver))
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
        public void redefineValue(Symbol symbol, Type type) {
            Optional<SymbolEntry> optionalEntry = getEntry(symbol);
            if (optionalEntry.isPresent()) {
                optionalEntry.get().redefineValue(type);
            } else {
                throw symbolNotFound(symbol);
            }
        }

        @Override
        public Type reserveType() {
            return parent.reserveType();
        }

        private SymbolEntry define(Symbol symbol) {
            return symbol.accept(new SymbolVisitor<SymbolEntry>() {
                @Override
                public SymbolEntry visit(QualifiedSymbol symbol) {
                    if (Objects.equals(moduleName, symbol.getModuleName())) {
                        return entries.computeIfAbsent(symbol, SymbolEntry::mutableEntry);
                    } else {
                        throw new UnsupportedOperationException(); // TODO
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
                    return Optional.empty();
                }
            });
        }

        @Override
        protected boolean isDefinedLocally(Symbol symbol) {
            return entries.containsKey(symbol);
        }
    }

    public static class RootScope extends Scope {

        private final TypeGenerator      typeGenerator;
        private final SymbolResolver     resolver;
        private final Map<String, Scope> children;

        private RootScope(TypeGenerator typeGenerator, SymbolResolver resolver) {
            this.typeGenerator = typeGenerator;
            this.resolver = new RootResolver(resolver);
            this.children = new HashMap<>();
        }

        @Override
        public void addPattern(Symbol symbol) {
            throw new IllegalStateException();
        }

        @Override
        public void bind(VariableType variableType, Type target) {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public void defineOperator(Symbol symbol, Operator operator) {
            throw new IllegalStateException();
        }

        @Override
        public void defineSignature(Symbol symbol, Type type) {
            throw new IllegalStateException();
        }

        @Override
        public void defineValue(Symbol symbol, Type type) {
            throw new IllegalStateException();
        }

        @Override
        public Scope enterScope() {
            throw new IllegalStateException();
        }

        @Override
        public Scope enterScope(String moduleName, List<Import> imports) {
            Scope scope = scope(this, resolver, moduleName, imports);
            children.put(moduleName, scope);
            return scope;
        }

        @Override
        public void extendContext(Type type, Set<Symbol> additionalContext) {
            throw new IllegalStateException();
        }

        @Override
        public Type generate(Type type) {
            throw new IllegalStateException();
        }

        @Override
        public Set<Symbol> getContext(Type type) {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public Operator getOperator(Symbol symbol) {
            throw new IllegalStateException();
        }

        @Override
        public Optional<Type> getSignature(Symbol symbol) {
            throw new IllegalStateException();
        }

        @Override
        public Type getTarget(Type type) {
            throw new IllegalStateException();
        }

        @Override
        public Type getValue(Symbol symbol) {
            throw new IllegalStateException();
        }

        @Override
        public boolean isBound(VariableType type) {
            throw new IllegalStateException();
        }

        @Override
        public boolean isDefined(Symbol symbol) {
            return resolver.isDefined(symbol);
        }

        @Override
        public boolean isOperator_(Symbol symbol) {
            return symbol.accept(new SymbolVisitor<Boolean>() {
                @Override
                public Boolean visit(QualifiedSymbol symbol) {
                    return children.containsKey(symbol.getModuleName()) && children.get(symbol.getModuleName()).isOperator_(symbol);
                }

                @Override
                public Boolean visit(UnqualifiedSymbol symbol) {
                    return false;
                }
            });
        }

        @Override
        public boolean isPattern(Symbol symbol) {
            return false;
        }

        @Override
        public Scope leaveScope() {
            throw new IllegalStateException("Can't leave root scope");
        }

        @Override
        public Optional<Symbol> qualify(Symbol symbol) {
            return symbol.accept(new SymbolVisitor<Optional<Symbol>>() {
                @Override
                public Optional<Symbol> visit(QualifiedSymbol symbol) {
                    if (resolver.isDefined(symbol)) {
                        return Optional.of(symbol);
                    } else {
                        throw symbolNotFound(symbol);
                    }
                }

                @Override
                public Optional<Symbol> visit(UnqualifiedSymbol symbol) {
                    throw symbolNotFound(symbol);
                }
            });
        }

        @Override
        public Symbol qualifyCurrent(Symbol symbol) {
            throw new IllegalStateException();
        }

        @Override
        public void redefineValue(Symbol symbol, Type type) {
            throw new IllegalStateException();
        }

        @Override
        public Type reserveType() {
            return typeGenerator.reserveType();
        }

        @Override
        protected Optional<SymbolEntry> getEntry(Symbol symbol) {
            return symbol.accept(new SymbolVisitor<Optional<SymbolEntry>>() {
                @Override
                public Optional<SymbolEntry> visit(QualifiedSymbol symbol) {
                    if (resolver.isDefined(symbol)) {
                        return Optional.of(resolver.getEntry(symbol));
                    } else {
                        return Optional.empty();
                    }
                }

                @Override
                public Optional<SymbolEntry> visit(UnqualifiedSymbol symbol) {
                    throw symbolNotFound(symbol);
                }
            });
        }

        @Override
        protected boolean isDefinedLocally(Symbol symbol) {
            return false;
        }

        private final class RootResolver implements SymbolResolver {

            private final SymbolResolver resolver;

            public RootResolver(SymbolResolver resolver) {
                this.resolver = resolver;
            }

            @Override
            public SymbolEntry getEntry(Symbol symbol) {
                return symbol.accept(new SymbolVisitor<SymbolEntry>() {
                    @Override
                    public SymbolEntry visit(QualifiedSymbol symbol) {
                        if (children.containsKey(symbol.getModuleName()) && children.get(symbol.getModuleName()).isDefinedLocally(symbol)) {
                            return children.get(symbol.getModuleName()).getEntry(symbol).orElseThrow(() -> symbolNotFound(symbol));
                        } else {
                            return resolver.getEntry(symbol);
                        }
                    }

                    @Override
                    public SymbolEntry visit(UnqualifiedSymbol symbol) {
                        throw symbolNotFound(symbol);
                    }
                });
            }

            @Override
            public boolean isDefined(Symbol symbol) {
                return symbol.accept(new SymbolVisitor<Boolean>() {
                    @Override
                    public Boolean visit(QualifiedSymbol symbol) {
                        return children.containsKey(symbol.getModuleName()) && children.get(symbol.getModuleName()).isDefinedLocally(symbol)
                            || resolver.isDefined(symbol);
                    }

                    @Override
                    public Boolean visit(UnqualifiedSymbol symbol) {
                        return false;
                    }
                });
            }
        }
    }
}
