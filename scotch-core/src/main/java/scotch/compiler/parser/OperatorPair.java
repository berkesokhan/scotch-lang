package scotch.compiler.parser;

import scotch.compiler.ast.Operator;

public class OperatorPair<T> {

    private final Operator operator;
    private final T        value;

    OperatorPair(Operator operator, T value) {
        this.operator = operator;
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public boolean hasLessPrecedenceThan(OperatorPair other) {
        return operator.hasLessPrecedenceThan(other.operator);
    }

    public boolean hasSamePrecedenceAs(OperatorPair other) {
        return operator.hasSamePrecedenceAs(other.operator);
    }

    public boolean isLeftAssociative() {
        return operator.isLeftAssociative();
    }

    public boolean isPrefix() {
        return operator.isPrefix();
    }
}
