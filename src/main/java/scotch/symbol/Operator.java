package scotch.symbol;

import static scotch.symbol.Value.Fixity.LEFT_INFIX;
import static scotch.symbol.Value.Fixity.PREFIX;
import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import scotch.symbol.Value.Fixity;

public class Operator {

    public static Operator operator(Fixity fixity, int precedence) {
        return new Operator(fixity, precedence);
    }

    private final Fixity fixity;
    private final int    precedence;

    private Operator(Fixity fixity, int precedence) {
        this.fixity = fixity;
        this.precedence = precedence;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof Operator) {
            Operator other = (Operator) o;
            return Objects.equals(fixity, other.fixity)
                && Objects.equals(precedence, other.precedence);
        } else {
            return false;
        }
    }

    public boolean hasLessPrecedenceThan(Operator other) {
        return precedence < other.precedence;
    }

    public boolean hasSamePrecedenceAs(Operator other) {
        return precedence == other.precedence;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fixity, precedence);
    }

    public boolean isLeftAssociative() {
        return fixity == LEFT_INFIX;
    }

    public boolean isPrefix() {
        return fixity == PREFIX;
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + fixity + ", " + precedence + ")";
    }
}
