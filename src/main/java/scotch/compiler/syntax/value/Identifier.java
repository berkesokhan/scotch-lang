package scotch.compiler.syntax.value;

import static java.util.Arrays.asList;
import static scotch.compiler.syntax.reference.DefinitionReference.valueRef;
import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Symbol.QualifiedSymbol;
import scotch.compiler.symbol.Symbol.SymbolVisitor;
import scotch.compiler.symbol.Symbol.UnqualifiedSymbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.syntax.BytecodeGenerator;
import scotch.compiler.syntax.SyntaxTreeParser;
import scotch.compiler.syntax.TypeChecker;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.text.SourceRange;

public class Identifier extends Value {

    private final SourceRange sourceRange;
    private final Symbol      symbol;
    private final Type        type;

    Identifier(SourceRange sourceRange, Symbol symbol, Type type) {
        this.sourceRange = sourceRange;
        this.symbol = symbol;
        this.type = type;
    }

    @Override
    public <T> T accept(ValueVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public Value accumulateDependencies(SyntaxTreeParser state) {
        return state.addDependency(this);
    }

    @Override
    public Value accumulateNames(SyntaxTreeParser state) {
        return this;
    }

    @Override
    public Value bindMethods(TypeChecker state) {
        return this;
    }

    @Override
    public Value checkTypes(TypeChecker state) {
        return bind(state.scope()).checkTypes(state);
    }

    @Override
    public Value bindTypes(TypeChecker state) {
        return withType(state.generate(type));
    }

    @Override
    public Value defineOperators(SyntaxTreeParser state) {
        return this;
    }

    @Override
    public Value parsePrecedence(SyntaxTreeParser state) {
        if (state.isOperator(symbol)) {
            return state.qualify(symbol)
                .map(this::withSymbol)
                .orElseGet(() -> {
                    state.symbolNotFound(symbol, sourceRange);
                    return this;
                });
        } else {
            return this;
        }
    }

    public Value bind(Scope scope) {
        Type valueType = scope.getValue(symbol);
        return symbol.accept(new SymbolVisitor<Value>() {
            @Override
            public Value visit(QualifiedSymbol symbol) {
                if (scope.isMember(symbol) || valueType.hasContext()) {
                    return unboundMethod(sourceRange, valueRef(symbol), valueType);
                } else {
                    return method(sourceRange, valueRef(symbol), asList(), valueType);
                }
            }

            @Override
            public Value visit(UnqualifiedSymbol symbol) {
                return arg(sourceRange, symbol.getMemberName(), valueType);
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof Identifier) {
            Identifier other = (Identifier) o;
            return Objects.equals(symbol, other.symbol)
                && Objects.equals(type, other.type);
        } else {
            return false;
        }
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SourceRange getSourceRange() {
        return sourceRange;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public Type getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, type);
    }

    @Override
    public Value qualifyNames(SyntaxTreeParser state) {
        return state.qualify(symbol)
            .map(this::withSymbol)
            .orElseGet(() -> {
                state.symbolNotFound(symbol, sourceRange);
                return this;
            });
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + symbol + ")";
    }

    public Identifier withSourceRange(SourceRange sourceRange) {
        return new Identifier(sourceRange, symbol, type);
    }

    public Identifier withSymbol(Symbol symbol) {
        return new Identifier(sourceRange, symbol, type);
    }

    public Identifier withType(Type type) {
        return new Identifier(sourceRange, symbol, type);
    }
}
