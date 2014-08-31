package scotch.lang;

import java.util.Objects;

public abstract class Either<L, R> {

    public static <L, R> Either<L, R> left(L left) {
        return new Left<>(left);
    }

    public static <L, R> Either<L, R> right(R right) {
        return new Right<>(right);
    }

    private Either() {
        // intentionally empty
    }

    @Override
    public abstract boolean equals(Object o);

    public abstract L getLeft();

    public abstract R getRight();

    @Override
    public abstract int hashCode();

    public abstract boolean isRight();

    @Override
    public abstract String toString();

    public static class Left<L, R> extends Either<L, R> {

        private final L left;

        private Left(L left) {
            this.left = left;
        }

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof Left && Objects.equals(left, ((Left) o).left);
        }

        @Override
        public L getLeft() {
            return left;
        }

        @Override
        public R getRight() {
            throw new IllegalStateException();
        }

        @Override
        public int hashCode() {
            return Objects.hash(left);
        }

        @Override
        public boolean isRight() {
            return false;
        }

        @Override
        public String toString() {
            return "Left(" + left + ")";
        }
    }

    public static class Right<L, R> extends Either<L, R> {

        private final R right;

        private Right(R right) {
            this.right = right;
        }

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof Right && Objects.equals(right, ((Right) o).right);
        }

        @Override
        public L getLeft() {
            throw new IllegalStateException();
        }

        @Override
        public R getRight() {
            return right;
        }

        @Override
        public int hashCode() {
            return Objects.hash(right);
        }

        @Override
        public boolean isRight() {
            return true;
        }

        @Override
        public String toString() {
            return "Right(" + right + ")";
        }
    }
}
