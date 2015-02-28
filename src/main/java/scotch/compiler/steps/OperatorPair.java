package scotch.compiler.steps;

import scotch.compiler.symbol.Operator;

class OperatorPair<T> {

    private final Operator operator;
    private final T        value;

    public OperatorPair(Operator operator, T value) {
        this.operator = operator;
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public boolean isLeftAssociative() {
        return operator.isLeftAssociative();
    }

    public boolean isLessPrecedentThan(OperatorPair<T> other) {
        return isLeftAssociative() && operator.hasSamePrecedenceAs(other.operator)
            || operator.hasLessPrecedenceThan(other.operator);
    }

    public boolean isPrefix() {
        return operator.isPrefix();
    }
}
