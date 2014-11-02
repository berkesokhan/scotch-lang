package scotch.compiler.ast;

import static scotch.compiler.ast.Symbol.fromString;
import static scotch.compiler.util.TextUtil.stringify;

import java.util.Objects;

public abstract class PatternMatch {

    public static PatternMatch capture(String name, Type type) {
        return capture(fromString(name), type);
    }

    public static PatternMatch capture(Symbol symbol, Type type) {
        return new CaptureMatch(symbol, type);
    }

    public static PatternMatch equal(Value value) {
        return new EqualMatch(value);
    }

    private PatternMatch() {
        // intentionally empty
    }

    public abstract <T> T accept(PatternMatchVisitor<T> visitor);

    @Override
    public abstract boolean equals(Object o);

    public abstract Type getType();

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();

    public interface PatternMatchVisitor<T> {

        default T visit(CaptureMatch match) {
            return visitOtherwise(match);
        }

        default T visit(EqualMatch match) {
            return visitOtherwise(match);
        }

        default T visitOtherwise(PatternMatch match) {
            throw new UnsupportedOperationException("Can't visit " + match);
        }
    }

    public static class CaptureMatch extends PatternMatch {

        private final Symbol symbol;
        private final Type   type;

        private CaptureMatch(Symbol symbol, Type type) {
            this.symbol = symbol;
            this.type = type;
        }

        @Override
        public <T> T accept(PatternMatchVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof CaptureMatch) {
                CaptureMatch other = (CaptureMatch) o;
                return Objects.equals(symbol, other.symbol)
                    && Objects.equals(type, other.type);
            } else {
                return false;
            }
        }

        public Symbol getSymbol() {
            return symbol;
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol, type);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + symbol + ")";
        }
    }

    public static class EqualMatch extends PatternMatch {

        private final Value value;

        public EqualMatch(Value value) {
            this.value = value;
        }

        @Override
        public <T> T accept(PatternMatchVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof EqualMatch && Objects.equals(value, ((EqualMatch) o).value);
        }

        @Override
        public Type getType() {
            return value.getType();
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + value + ")";
        }
    }
}
