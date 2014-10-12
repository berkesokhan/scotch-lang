package scotch.compiler.ast;

import static scotch.compiler.util.TextUtil.stringify;

import java.util.Objects;
import scotch.lang.Type;

public abstract class PatternMatch {

    public static PatternMatch capture(String name, Type type) {
        return new CaptureMatch(name, type);
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

        private final String name;
        private final Type   type;

        private CaptureMatch(String name, Type type) {
            this.name = name;
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
                return Objects.equals(name, other.name)
                    && Objects.equals(type, other.type);
            } else {
                return false;
            }
        }

        public String getName() {
            return name;
        }

        public Type getType() {
            return type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + name + ")";
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
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + value + ")";
        }
    }
}
