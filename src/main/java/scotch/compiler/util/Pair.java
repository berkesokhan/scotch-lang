package scotch.compiler.util;

import java.util.Objects;

public class Pair<A, B> {

    public static <A, B> Pair<A, B> pair(A left, B right) {
        return new Pair<>(left, right);
    }

    private final A left;
    private final B right;

    private Pair(A left, B right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof Pair) {
            Pair other = (Pair) o;
            return Objects.equals(left, other.left)
                && Objects.equals(right, other.right);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right);
    }

    public <C> C into(Destructure<A, B, C> destructure) {
        return destructure.apply(left, right);
    }

    @Override
    public String toString() {
        return "Pair(" + left + ", " + right + ")";
    }

    @FunctionalInterface
    public interface Destructure<A, B, C> {

        C apply(A left, B right);
    }
}
