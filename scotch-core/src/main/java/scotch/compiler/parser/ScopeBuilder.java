package scotch.compiler.parser;

import static scotch.compiler.ast.DefinitionEntry.patternEntry;
import static scotch.compiler.ast.DefinitionEntry.scopedEntry;
import static scotch.compiler.ast.Scope.scope;
import static scotch.compiler.ast.Type.t;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import com.google.common.collect.ImmutableList;
import scotch.compiler.ast.Definition;
import scotch.compiler.ast.Definition.DefinitionVisitor;
import scotch.compiler.ast.Definition.OperatorDefinition;
import scotch.compiler.ast.DefinitionEntry;
import scotch.compiler.ast.DefinitionReference;
import scotch.compiler.ast.Import;
import scotch.compiler.ast.Operator;
import scotch.compiler.ast.PatternMatcher;
import scotch.compiler.ast.Scope;
import scotch.compiler.ast.Symbol;
import scotch.compiler.ast.Symbol.QualifiedSymbol;
import scotch.compiler.ast.Symbol.SymbolVisitor;
import scotch.compiler.ast.Symbol.UnqualifiedSymbol;
import scotch.compiler.ast.SymbolResolver;
import scotch.compiler.ast.Type;

public class ScopeBuilder {

    private final Map<DefinitionReference, DefinitionEntry> definitions;
    private       String                                    currentModule;
    private final Deque<Set<Symbol>>                        patterns;
    private       Scope                                     scope;
    private       int                                       sequence;

    public ScopeBuilder(int sequence, SymbolResolver resolver) {
        this.definitions = new HashMap<>();
        this.patterns = new ArrayDeque<>();
        this.scope = scope(resolver);
        this.sequence = sequence;
    }

    public void addPattern(Symbol symbol) {
        patterns.peek().add(symbol);
    }

    public Scope build() {
        return scope;
    }

    public PatternMatcher collect(PatternMatcher pattern) {
        definitions.put(pattern.getReference(), patternEntry(pattern, build()));
        return pattern;
    }

    public Definition collect(Definition definition) {
        definitions.put(definition.getReference(), scopedEntry(definition, build()));
        return definition;
    }

    public void defineOperator(Symbol symbol, Definition definition) {
        definition.accept(new DefinitionVisitor<Void>() {
            @Override
            public Void visit(OperatorDefinition definition) {
                scope.defineOperator(symbol, definition.getOperator());
                return null;
            }

            @Override
            public Void visitOtherwise(Definition definition) {
                throw new UnsupportedOperationException("Can't define operator for " + definition.getClass().getSimpleName());
            }
        });
    }

    public void defineValue(Symbol symbol, Type type) {
        scope.defineValue(symbol, type);
    }

    public void enterScope() {
        patterns.push(new HashSet<>());
        scope = scope.enterScope();
    }

    public void enterScope(List<Import> imports) {
        patterns.push(new HashSet<>());
        scope = scope.enterScope(currentModule, imports);
    }

    public DefinitionEntry getDefinition(DefinitionReference reference) {
        return definitions.get(reference);
    }

    public List<DefinitionEntry> getDefinitions() {
        return ImmutableList.copyOf(definitions.values());
    }

    public Operator getOperator(Symbol symbol) {
        return scope.getOperator(symbol);
    }

    public int getSequence() {
        return sequence;
    }

    public <T> T hoist(Supplier<T> supplier) {
        Scope childScope = scope;
        Set<Symbol> childPatterns = patterns.peek();
        leaveScope();
        try {
            return supplier.get();
        } finally {
            scope = childScope;
            patterns.push(childPatterns);
        }
    }

    public boolean isOperator(Symbol symbol) {
        return qualify(symbol).map(scope::isOperator).orElse(false);
    }

    public boolean isPattern(Symbol symbol) {
        return patterns.peek().contains(symbol);
    }

    public void leaveScope() {
        patterns.pop();
        scope = scope.leaveScope();
    }

    public Optional<Symbol> qualify(Symbol symbol) {
        return scope.qualify(symbol);
    }

    public Symbol qualifyLocal(Symbol symbol) {
        return symbol.accept(new SymbolVisitor<Symbol>() {
            @Override
            public Symbol visit(QualifiedSymbol symbol) {
                throw new UnsupportedOperationException(); // TODO
            }

            @Override
            public Symbol visit(UnqualifiedSymbol symbol) {
                return symbol.qualifyWith(currentModule);
            }
        });
    }

    public Symbol qualifyCurrent(Symbol symbol) {
        return symbol.qualifyWith(currentModule);
    }

    public Type reserveType() {
        return t(sequence++);
    }

    public void setCurrentModule(String currentModule) {
        this.currentModule = currentModule;
    }
}
