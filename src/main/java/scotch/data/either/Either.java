package scotch.data.either;

import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import java.util.function.Function;

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

    public abstract <T> T accept(EitherVisitor<L, R, T> visitor);

    @Override
    public abstract boolean equals(Object o);

    public abstract L getLeft();

    public abstract R getRight();

    public abstract R getRightOr(Function<L, R> function);

    @Override
    public abstract int hashCode();

    public boolean isLeft() {
        return !isRight();
    }

    public abstract boolean isRight();

    @Override
    public abstract String toString();

    public interface EitherVisitor<L, R, T> {

        T visitLeft(L left);

        T visitRight(R right);
    }

    public static class Left<L, R> extends Either<L, R> {

        private final L left;

        private Left(L left) {
            this.left = left;
        }

        @Override
        public <T> T accept(EitherVisitor<L, R, T> visitor) {
            return visitor.visitLeft(left);
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
        public R getRightOr(Function<L, R> function) {
            return function.apply(left);
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
            return stringify(this) + "(" + left + ")";
        }
    }

    public static class Right<L, R> extends Either<L, R> {

        private final R right;

        private Right(R right) {
            this.right = right;
        }

        @Override
        public <T> T accept(EitherVisitor<L, R, T> visitor) {
            return visitor.visitRight(right);
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
        public R getRightOr(Function<L, R> supplier) {
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
            return stringify(this) + "(" + right + ")";
        }
    }
}
