package scotch.compiler.syntax;

import java.util.Objects;

public abstract class SyntaxError {

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

    @Override
    public abstract String toString();

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
        public String toString() {
            return "TypeError(" + unification + ", " + sourceRange + ")";
        }
    }
}
