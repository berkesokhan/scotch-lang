package scotch.compiler.parser;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static scotch.compiler.ast.Operator.Fixity.LEFT_INFIX;
import static scotch.compiler.ast.Operator.operator;
import static scotch.compiler.ast.Scope.scope;
import static scotch.compiler.util.TextUtil.containsSymbols;
import static scotch.compiler.util.TextUtil.quote;
import static scotch.lang.Type.t;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import scotch.compiler.ast.Definition;
import scotch.compiler.ast.Definition.DefinitionVisitor;
import scotch.compiler.ast.Definition.OperatorDefinition;
import scotch.compiler.ast.Operator;
import scotch.compiler.ast.Scope;
import scotch.compiler.ast.Symbol;
import scotch.lang.Type;

public class ScopeBuilder {

    private static final Operator defaultOperator = operator(LEFT_INFIX, 7);
    private final Map<String, Operator> operators;
    private Scope scope = scope(empty());
    private int nextId;

    public ScopeBuilder() {
        operators = new HashMap<>();
    }

    public void define(Definition definition) {
        definition.accept(new DefinitionVisitor<Void>() {
            @Override
            public Void visit(OperatorDefinition definition) {
                operators.put(definition.getName(), operator(definition.getFixity(), definition.getPrecedence()));
                return null;
            }
        });
    }

    public void define(String name, Type type) {
        scope.define(name, type);
    }

    public void enterScope() {
        scope = scope(of(scope));
    }

    public Symbol forwardReference(String name) {
        return scope.forwardReference(name);
    }

    public Operator getOperator(String name) {
        if (isOperator(name)) {
            return operators.getOrDefault(name, defaultOperator);
        } else {
            throw new IllegalArgumentException("No operator found for symbol " + quote(name));
        }
    }

    public Scope getScope() {
        return scope;
    }

    public Type getType(String name) {
        return scope.getType(name);
    }

    public Type getValueType(String name) {
        if (isDefined(name)) {
            return scope.getValueType(name);
        } else {
            Symbol symbol = forwardReference(name);
            if (!symbol.hasValueType()) {
                symbol.setValueType(reserveType());
            }
            return symbol.getValueType();
        }
    }

    public boolean isDefined(String name) {
        return scope.isDefined(name);
    }

    public boolean isLocal(String name) {
        return scope.isLocal(name);
    }

    public boolean isOperator(String name) {
        return operators.containsKey(name) || isDefaultOperator(name);
    }

    public void leaveScope() {
        scope = scope.getParent();
    }

    public <T> T pullUp(String name, Supplier<T> supplier) {
        Scope currentScope = scope;
        currentScope.undefine(name);
        scope = scope.getParent();
        try {
            return supplier.get();
        } finally {
            scope = currentScope;
        }
    }

    public Type reserveType() {
        return t(nextId++);
    }

    public void undefine(String name) {
        scope.undefine(name);
    }

    private boolean isDefaultOperator(String name) {
        return containsSymbols(name);
    }
}
