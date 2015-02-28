package scotch.compiler.syntax.scope;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import scotch.compiler.symbol.MethodSignature;
import scotch.compiler.symbol.Operator;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Symbol.QualifiedSymbol;
import scotch.compiler.symbol.Symbol.SymbolVisitor;
import scotch.compiler.symbol.Symbol.UnqualifiedSymbol;
import scotch.compiler.symbol.SymbolEntry;
import scotch.compiler.symbol.SymbolResolver;
import scotch.compiler.symbol.descriptor.DataConstructorDescriptor;
import scotch.compiler.symbol.descriptor.DataTypeDescriptor;
import scotch.compiler.symbol.descriptor.TypeClassDescriptor;
import scotch.compiler.symbol.descriptor.TypeInstanceDescriptor;
import scotch.compiler.symbol.type.SumType;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.symbol.type.Unification;
import scotch.compiler.symbol.type.VariableType;
import scotch.compiler.symbol.util.SymbolGenerator;
import scotch.compiler.syntax.definition.Import;
import scotch.compiler.syntax.reference.ClassReference;
import scotch.compiler.syntax.reference.ValueReference;
import scotch.compiler.syntax.value.PatternMatcher;

public class RootScope extends Scope {

    private final SymbolGenerator    symbolGenerator;
    private final RootResolver       resolver;
    private final Map<String, Scope> children;

    RootScope(SymbolGenerator symbolGenerator, SymbolResolver resolver) {
        this.symbolGenerator = symbolGenerator;
        this.resolver = new RootResolver(resolver);
        this.children = new HashMap<>();
    }

    @Override
    public void addDependency(Symbol symbol) {
        throw new IllegalStateException();
    }

    @Override
    public void addPattern(Symbol symbol, PatternMatcher pattern) {
        throw new IllegalStateException();
    }

    @Override
    public void defineDataType(Symbol symbol, DataTypeDescriptor descriptor) {
        throw new IllegalStateException();
    }

    @Override
    public void defineDataConstructor(Symbol symbol, DataConstructorDescriptor descriptor) {
        throw new IllegalStateException();
    }

    @Override
    public Unification bind(VariableType variableType, Type target) {
        throw new IllegalStateException();
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
        Scope scope = scope(this, new DefaultTypeScope(symbolGenerator, resolver), resolver, moduleName, imports);
        children.put(moduleName, scope);
        return scope;
    }

    @Override
    public void extendContext(Type type, Set<Symbol> additionalContext) {
        throw new IllegalStateException();
    }

    @Override
    public void generalize(Type type) {
        throw new IllegalStateException();
    }

    @Override
    public Type generate(Type type) {
        throw new IllegalStateException();
    }

    @Override
    public Set<Symbol> getContext(Type type) {
        throw new IllegalStateException();
    }

    @Override
    public Set<Symbol> getDependencies() {
        throw new IllegalStateException();
    }

    @Override
    public Optional<TypeClassDescriptor> getMemberOf(ValueReference valueRef) {
        throw new IllegalStateException();
    }

    @Override
    public Optional<Operator> getOperator(Symbol symbol) {
        throw new IllegalStateException();
    }

    @Override
    public Scope getParent() {
        throw new IllegalStateException();
    }

    @Override
    public Map<Symbol, List<PatternMatcher>> getPatterns() {
        throw new IllegalStateException();
    }

    @Override
    public Optional<Type> getRawValue(Symbol symbol) {
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
    public void implement(Symbol typeClass, SumType type) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public Optional<TypeClassDescriptor> getTypeClass(ClassReference classRef) {
        throw new IllegalStateException();
    }

    @Override
    public Set<TypeInstanceDescriptor> getTypeInstances(Symbol typeClass, List<Type> parameters) {
        throw new IllegalStateException();
    }

    @Override
    public Optional<MethodSignature> getValueSignature(Symbol symbol) {
        return resolver.getEntry(symbol).flatMap(SymbolEntry::getValueMethod);
    }

    @Override
    public boolean isBound(VariableType type) {
        throw new IllegalStateException();
    }

    @Override
    public boolean isGeneric(VariableType variableType) {
        throw new IllegalStateException();
    }

    @Override
    public boolean isImplemented(Symbol typeClass, SumType type) {
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
                    return Optional.empty();
                }
            }

            @Override
            public Optional<Symbol> visit(UnqualifiedSymbol symbol) {
                return Optional.empty();
            }
        });
    }

    @Override
    public Symbol qualifyCurrent(Symbol symbol) {
        throw new IllegalStateException();
    }

    @Override
    protected String getModuleName() {
        throw new IllegalStateException();
    }

    @Override
    public Symbol reserveSymbol() {
        return symbolGenerator.reserveSymbol();
    }

    @Override
    public Symbol reserveSymbol(List<String> nestings) {
        return symbolGenerator.reserveSymbol(nestings);
    }

    @Override
    public VariableType reserveType() {
        return symbolGenerator.reserveType();
    }

    @Override
    public void specialize(Type type) {
        throw new IllegalStateException();
    }

    @Override
    protected Optional<SymbolEntry> getEntry(Symbol symbol) {
        return symbol.accept(new SymbolVisitor<Optional<SymbolEntry>>() {
            @Override
            public Optional<SymbolEntry> visit(QualifiedSymbol symbol) {
                return resolver.getEntry(symbol);
            }

            @Override
            public Optional<SymbolEntry> visit(UnqualifiedSymbol symbol) {
                return Optional.empty();
            }
        });
    }

    @Override
    protected boolean isDataConstructor(Symbol symbol) {
        return false;
    }

    @Override
    protected boolean isDefinedLocally(Symbol symbol) {
        return false;
    }

    @Override
    protected boolean isExternal(Symbol symbol) {
        return resolver.isExternal(symbol);
    }

    private final class RootResolver implements SymbolResolver {

        private final SymbolResolver resolver;

        public RootResolver(SymbolResolver resolver) {
            this.resolver = resolver;
        }

        @Override
        public Optional<SymbolEntry> getEntry(Symbol symbol) {
            return symbol.accept(new SymbolVisitor<Optional<SymbolEntry>>() {
                @Override
                public Optional<SymbolEntry> visit(QualifiedSymbol symbol) {
                    if (children.containsKey(symbol.getModuleName()) && children.get(symbol.getModuleName()).isDefinedLocally(symbol)) {
                        return children.get(symbol.getModuleName()).getEntry(symbol);
                    } else {
                        return resolver.getEntry(symbol);
                    }
                }

                @Override
                public Optional<SymbolEntry> visit(UnqualifiedSymbol symbol) {
                    return Optional.empty();
                }
            });
        }

        @Override
        public Set<TypeInstanceDescriptor> getTypeInstances(Symbol symbol, List<Type> types) {
            return resolver.getTypeInstances(symbol, types);
        }

        @Override
        public Set<TypeInstanceDescriptor> getTypeInstancesByModule(String moduleName) {
            return resolver.getTypeInstancesByModule(moduleName);
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

        public boolean isExternal(Symbol symbol) {
            return resolver.isDefined(symbol);
        }
    }
}
