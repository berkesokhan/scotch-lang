package scotch.compiler.syntax;

import static scotch.compiler.syntax.SourceRange.NULL_SOURCE;
import static scotch.compiler.syntax.Symbol.fromString;
import static scotch.compiler.util.TextUtil.stringify;

import java.util.Objects;

public abstract class PatternMatch implements SourceAware<PatternMatch> {

    public static PatternMatch capture(String name, Type type) {
        return capture(fromString(name), type);
    }

    public static PatternMatch capture(Symbol symbol, Type type) {
        return new CaptureMatch(NULL_SOURCE, symbol, type);
    }

    public static PatternMatch equal(Value value) {
        return new EqualMatch(NULL_SOURCE, value);
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

        T visitOtherwise(PatternMatch match);
    }

    public static class CaptureMatch extends PatternMatch {

        private final SourceRange sourceRange;
        private final Symbol      symbol;
        private final Type        type;

        private CaptureMatch(SourceRange sourceRange, Symbol symbol, Type type) {
            this.sourceRange = sourceRange;
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

        @Override
        public SourceRange getSourceRange() {
            return sourceRange;
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

        @Override
        public CaptureMatch withSourceRange(SourceRange sourceRange) {
            return new CaptureMatch(sourceRange, symbol, type);
        }
    }

    public static class EqualMatch extends PatternMatch {

        private final SourceRange sourceRange;
        private final Value       value;

        public EqualMatch(SourceRange sourceRange, Value value) {
            this.sourceRange = sourceRange;
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
        public SourceRange getSourceRange() {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public Type getType() {
            return value.getType();
        }

        public Value getValue() {
            return value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + value + ")";
        }

        @Override
        public EqualMatch withSourceRange(SourceRange sourceRange) {
            return new EqualMatch(sourceRange, value);
        }

        public EqualMatch withValue(Value value) {
            return new EqualMatch(sourceRange, value);
        }
    }
}
