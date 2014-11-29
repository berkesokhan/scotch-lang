package scotch.compiler.syntax;

import static scotch.compiler.util.TextUtil.quote;
import static scotch.compiler.util.TextUtil.stringify;

import java.util.Objects;

public abstract class SyntaxError {

    public static SyntaxError parseError(String description, SourceRange location) {
        return new ParseError(description, location);
    }

    public static SyntaxError symbolNotFound(Symbol symbol, SourceRange location) {
        return new SymbolNotFoundError(symbol, location);
    }

    public static SyntaxError typeError(Unification unification, SourceRange location) {
        return new TypeError(unification, location);
    }

    private SyntaxError() {
        // intentionally empty
    }

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    public abstract String prettyPrint();

    @Override
    public abstract String toString();

    public static class ParseError extends SyntaxError {

        private final String      description;
        private final SourceRange sourceRange;

        private ParseError(String description, SourceRange sourceRange) {
            this.description = description;
            this.sourceRange = sourceRange;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof ParseError) {
                ParseError other = (ParseError) o;
                return Objects.equals(description, other.description)
                    && Objects.equals(sourceRange, other.sourceRange);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(description, sourceRange);
        }

        @Override
        public String prettyPrint() {
            return description + " " + sourceRange.prettyPrint();
        }

        @Override
        public String toString() {
            return stringify(this) + "(description=" + quote(description) + ")";
        }
    }

    public static class SymbolNotFoundError extends SyntaxError {

        private final Symbol      symbol;
        private final SourceRange sourceRange;

        private SymbolNotFoundError(Symbol symbol, SourceRange sourceRange) {
            this.symbol = symbol;
            this.sourceRange = sourceRange;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof SymbolNotFoundError) {
                SymbolNotFoundError other = (SymbolNotFoundError) o;
                return Objects.equals(symbol, other.symbol)
                    && Objects.equals(sourceRange, other.sourceRange);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol, sourceRange);
        }

        @Override
        public String prettyPrint() {
            return "Symbol not found: " + symbol.quote() + " " + sourceRange.prettyPrint();
        }

        @Override
        public String toString() {
            return stringify(this) + "(symbol=" + symbol + ")";
        }
    }

    public static class TypeError extends SyntaxError {

        private final Unification unification;
        private final SourceRange sourceRange;

        private TypeError(Unification unification, SourceRange sourceRange) {
            this.unification = unification;
            this.sourceRange = sourceRange;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof TypeError) {
                TypeError other = (TypeError) o;
                return Objects.equals(unification, other.unification)
                    && Objects.equals(sourceRange, other.sourceRange);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(unification, sourceRange);
        }

        @Override
        public String prettyPrint() {
            return unification.prettyPrint() + " " + sourceRange.prettyPrint();
        }

        @Override
        public String toString() {
            return "TypeError(" + unification + ")";
        }
    }
}
