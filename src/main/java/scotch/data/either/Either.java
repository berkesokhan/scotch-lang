package scotch.data.either;

import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import java.util.function.Function;

public abstract class Either<A, B> {

    public static <A, B> Either<A, B> left(A left) {
        return new Left<>(left);
    }

    public static <A, B> Either<A, B> right(B right) {
        return new Right<>(right);
    }

    private Either() {
        // intentionally empty
    }

    @Override
    public abstract boolean equals(Object o);

    public abstract A getLeft();

    public abstract B getRight();

    public abstract B getRightOr(Function<A, B> function);

    @Override
    public abstract int hashCode();

    public boolean isLeft() {
        return !isRight();
    }

    public abstract boolean isRight();

    public abstract <C> Either<A, C> map(Function<? super B, ? extends C> function);

    public abstract B orElseGet(Function<A, B> function);

    public abstract <T extends RuntimeException> B orElseThrow(Function<? super A, ? extends T> function) throws T;

    @Override
    public abstract String toString();

    public static class Left<A, B> extends Either<A, B> {

        private final A value;

        private Left(A value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof Left && Objects.equals(value, ((Left) o).value);
        }

        @Override
        public A getLeft() {
            return value;
        }

        @Override
        public B getRight() {
            throw new IllegalStateException();
        }

        @Override
        public B getRightOr(Function<A, B> function) {
            return function.apply(value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public boolean isRight() {
            return false;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <C> Either<A, C> map(Function<? super B, ? extends C> function) {
            return (Either<A, C>) this;
        }

        @Override
        public B orElseGet(Function<A, B> function) {
            return function.apply(value);
        }

        @Override
        public <T extends RuntimeException> B orElseThrow(Function<? super A, ? extends T> function) throws T {
            throw function.apply(value);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + value + ")";
        }
    }

    public static class Right<A, B> extends Either<A, B> {

        private final B value;

        private Right(B value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof Right && Objects.equals(value, ((Right) o).value);
        }

        @Override
        public A getLeft() {
            throw new IllegalStateException();
        }

        @Override
        public B getRight() {
            return value;
        }

        @Override
        public B getRightOr(Function<A, B> supplier) {
            return value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public boolean isRight() {
            return true;
        }

        @Override
        public <C> Either<A, C> map(Function<? super B, ? extends C> function) {
            return right(function.apply(value));
        }

        @Override
        public B orElseGet(Function<A, B> function) {
            return value;
        }

        @Override
        public <T extends RuntimeException> B orElseThrow(Function<? super A, ? extends T> function) throws T {
            return value;
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + value + ")";
        }
    }
}
