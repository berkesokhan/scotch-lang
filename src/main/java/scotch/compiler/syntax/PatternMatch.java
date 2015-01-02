package scotch.compiler.syntax;

import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import java.util.Optional;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.text.SourceRange;

public abstract class PatternMatch {

    public static CaptureMatch capture(SourceRange sourceRange, Optional<String> argument, Symbol symbol, Type type) {
        return new CaptureMatch(sourceRange, argument, symbol, type);
    }

    public static EqualMatch equal(SourceRange sourceRange, Optional<String> argument, Value value) {
        return new EqualMatch(sourceRange, argument, value);
    }

    private PatternMatch() {
        // intentionally empty
    }

    public abstract <T> T accept(PatternMatchVisitor<T> visitor);

    public abstract PatternMatch bind(String argument);

    @Override
    public abstract boolean equals(Object o);

    public abstract SourceRange getSourceRange();

    public abstract Type getType();

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();

    public abstract PatternMatch withType(Type generate);

    public interface PatternMatchVisitor<T> {

        default T visit(CaptureMatch match) {
            return visitOtherwise(match);
        }

        default T visit(EqualMatch match) {
            return visitOtherwise(match);
        }

        default T visitOtherwise(PatternMatch match) {
            throw new UnsupportedOperationException("Can't visit " + match.getClass().getSimpleName());
        }
    }

    public static class CaptureMatch extends PatternMatch {

        private final SourceRange      sourceRange;
        private final Optional<String> argument;
        private final Symbol           symbol;
        private final Type             type;

        private CaptureMatch(SourceRange sourceRange, Optional<String> argument, Symbol symbol, Type type) {
            this.sourceRange = sourceRange;
            this.argument = argument;
            this.symbol = symbol;
            this.type = type;
        }

        @Override
        public <T> T accept(PatternMatchVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public PatternMatch bind(String argument) {
            if (this.argument.isPresent()) {
                throw new IllegalStateException();
            } else {
                return capture(sourceRange, Optional.of(argument), symbol, type);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof CaptureMatch) {
                CaptureMatch other = (CaptureMatch) o;
                return Objects.equals(argument, other.argument)
                    && Objects.equals(symbol, other.symbol)
                    && Objects.equals(type, other.type);
            } else {
                return false;
            }
        }

        public String getArgument() {
            return argument.orElseThrow(IllegalStateException::new);
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
            return Objects.hash(argument, symbol, type);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + symbol + ")";
        }

        public CaptureMatch withSourceRange(SourceRange sourceRange) {
            return new CaptureMatch(sourceRange, argument, symbol, type);
        }

        @Override
        public PatternMatch withType(Type type) {
            return new CaptureMatch(sourceRange, argument, symbol, type);
        }
    }

    public static class EqualMatch extends PatternMatch {

        private final SourceRange      sourceRange;
        private final Optional<String> argument;
        private final Value            value;

        public EqualMatch(SourceRange sourceRange, Optional<String> argument, Value value) {
            this.sourceRange = sourceRange;
            this.argument = argument;
            this.value = value;
        }

        @Override
        public <T> T accept(PatternMatchVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public PatternMatch bind(String argument) {
            if (this.argument.isPresent()) {
                throw new IllegalStateException();
            } else {
                return new EqualMatch(sourceRange, Optional.of(argument), value);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof EqualMatch) {
                EqualMatch other = (EqualMatch) o;
                return Objects.equals(sourceRange, other.sourceRange)
                    && Objects.equals(argument, other.argument)
                    && Objects.equals(value, other.value);
            } else {
                return false;
            }
        }

        @Override
        public SourceRange getSourceRange() {
            return sourceRange;
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
            return Objects.hash(argument, value);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + value + ")";
        }

        @Override
        public EqualMatch withType(Type generate) {
            return new EqualMatch(sourceRange, argument, value);
        }

        public EqualMatch withSourceRange(SourceRange sourceRange) {
            return new EqualMatch(sourceRange, argument, value);
        }

        public EqualMatch withValue(Value value) {
            return new EqualMatch(sourceRange, argument, value);
        }
    }
}
