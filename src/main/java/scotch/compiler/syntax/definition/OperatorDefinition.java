package scotch.compiler.syntax.definition;

import static scotch.compiler.symbol.Operator.operator;
import static scotch.compiler.syntax.reference.DefinitionReference.operatorRef;
import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import java.util.Optional;
import scotch.compiler.symbol.Operator;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Value.Fixity;
import scotch.compiler.syntax.BytecodeGenerator;
import scotch.compiler.syntax.SyntaxTreeParser;
import scotch.compiler.syntax.TypeChecker;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.text.SourceRange;

public class OperatorDefinition extends Definition {

    private final SourceRange sourceRange;
    private final Symbol      symbol;
    private final Fixity      fixity;
    private final int         precedence;

    OperatorDefinition(SourceRange sourceRange, Symbol symbol, Fixity fixity, int precedence) {
        this.sourceRange = sourceRange;
        this.symbol = symbol;
        this.fixity = fixity;
        this.precedence = precedence;
    }

    @Override
    public <T> T accept(DefinitionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public Definition accumulateDependencies(SyntaxTreeParser state) {
        return state.keep(this);
    }

    @Override
    public Definition accumulateNames(SyntaxTreeParser state) {
        return state.keep(this);
    }

    @Override
    public Definition bindTypes(TypeChecker state) {
        return state.keep(this);
    }

    @Override
    public Definition checkTypes(TypeChecker state) {
        return state.keep(this);
    }

    @Override
    public Definition defineOperators(SyntaxTreeParser state) {
        return state.scoped(this, () -> {
            state.defineOperator(symbol, getOperator());
            return this;
        });
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof OperatorDefinition) {
            OperatorDefinition other = (OperatorDefinition) o;
            return Objects.equals(symbol, other.symbol)
                && Objects.equals(fixity, other.fixity)
                && Objects.equals(precedence, other.precedence);
        } else {
            return false;
        }
    }

    @Override
    public void generateBytecode(BytecodeGenerator state) {
        throw new UnsupportedOperationException(); // TODO
    }

    public Operator getOperator() {
        return operator(fixity, precedence);
    }

    @Override
    public DefinitionReference getReference() {
        return operatorRef(symbol);
    }

    @Override
    public SourceRange getSourceRange() {
        return sourceRange;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, fixity, precedence);
    }

    @Override
    public Optional<Definition> parsePrecedence(SyntaxTreeParser state) {
        return Optional.of(state.collect(this));
    }

    @Override
    public Definition qualifyNames(SyntaxTreeParser state) {
        return state.keep(this);
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + symbol + " :: " + fixity + ", " + precedence + ")";
    }
}
