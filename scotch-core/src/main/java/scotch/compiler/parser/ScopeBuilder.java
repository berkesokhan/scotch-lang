package scotch.compiler.parser;

import static scotch.compiler.ast.DefinitionEntry.scopedEntry;
import static scotch.compiler.ast.Scope.scope;
import static scotch.lang.Type.t;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.google.common.collect.ImmutableList;
import scotch.compiler.ast.Definition;
import scotch.compiler.ast.Definition.DefinitionVisitor;
import scotch.compiler.ast.Definition.OperatorDefinition;
import scotch.compiler.ast.DefinitionEntry;
import scotch.compiler.ast.DefinitionReference;
import scotch.compiler.ast.Operator;
import scotch.compiler.ast.Scope;
import scotch.lang.Symbol;
import scotch.lang.Symbol.QualifiedSymbol;
import scotch.lang.Symbol.SymbolVisitor;
import scotch.lang.Symbol.UnqualifiedSymbol;
import scotch.lang.Type;

public class ScopeBuilder {

    private final Map<DefinitionReference, DefinitionEntry> definitions;
    private       String                                    currentModule;
    private final Deque<Set<Symbol>>                        patterns;
    private       Scope                                     scope;
    private       int                                       sequence;

    public ScopeBuilder(int sequence) {
        this.definitions = new HashMap<>();
        this.patterns = new ArrayDeque<>();
        this.scope = scope();
        this.sequence = sequence;
    }

    public void addPattern(Symbol symbol) {
        patterns.peek().add(symbol);
    }

    public Scope build() {
        return scope;
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

    public boolean isOperator(Symbol symbol) {
        return scope.isOperator(qualify(symbol));
    }

    public boolean isPattern(Symbol symbol) {
        return patterns.peek().contains(symbol);
    }

    public void leaveScope() {
        patterns.pop();
        scope = scope.leaveScope();
    }

    public Symbol qualify(Symbol symbol) {
        return symbol.accept(new SymbolVisitor<Symbol>() {
            @Override
            public Symbol visit(QualifiedSymbol symbol) {
                return symbol;
            }

            @Override
            public Symbol visit(UnqualifiedSymbol symbol) {
                return scope.qualify(symbol);
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
