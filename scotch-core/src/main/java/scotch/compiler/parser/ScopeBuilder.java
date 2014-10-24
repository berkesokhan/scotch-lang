package scotch.compiler.parser;

import static scotch.compiler.ast.Scope.scope;

import scotch.compiler.ast.Definition;
import scotch.compiler.ast.Definition.DefinitionVisitor;
import scotch.compiler.ast.Definition.OperatorDefinition;
import scotch.compiler.ast.Operator;
import scotch.compiler.ast.Scope;
import scotch.lang.Symbol;
import scotch.lang.Symbol.QualifiedSymbol;
import scotch.lang.Symbol.SymbolVisitor;
import scotch.lang.Symbol.UnqualifiedSymbol;
import scotch.lang.Type;

public class ScopeBuilder {

    private Scope scope;

    public ScopeBuilder() {
        scope = scope();
    }

    public Scope build() {
        return scope;
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

    public void enterScope(String moduleName) {
        scope = scope.enterScope(moduleName);
    }

    public Operator getOperator(Symbol symbol) {
        return scope.getOperator(symbol);
    }

    public boolean isOperator(Symbol symbol) {
        return scope.isOperator(qualify(symbol));
    }

    public void leaveScope() {
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
}
