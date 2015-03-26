package scotch.data.either;

import static java.util.Arrays.asList;
import static scotch.runtime.RuntimeSupport.applicable;
import static scotch.runtime.RuntimeSupport.callable;
import static scotch.symbol.type.Types.fn;
import static scotch.symbol.type.Types.sum;
import static scotch.symbol.type.Types.var;

import java.util.List;
import java.util.Objects;
import scotch.runtime.Applicable;
import scotch.runtime.Callable;
import scotch.runtime.RuntimeSupport;
import scotch.symbol.DataConstructor;
import scotch.symbol.DataType;
import scotch.symbol.TypeParameter;
import scotch.symbol.TypeParameters;
import scotch.symbol.Value;
import scotch.symbol.ValueType;
import scotch.symbol.type.Type;

@SuppressWarnings("unused")
@DataType(memberName = "Either", parameters = {
    @TypeParameter(name = "a"),
    @TypeParameter(name = "b"),
})
public abstract class Either<A, B> {

    @Value(memberName = "Left")
    public static <A, B> Applicable<A, Either<A, B>> left() {
        return applicable(value -> callable(() -> new Left<>(value)));
    }

    public static <A, B> Either<A, B> left(A value) {
        return Either.<A, B>left().apply(RuntimeSupport.box(value)).call();
    }

    @ValueType(forMember = "Left")
    public static Type left$type() {
        return fn(var("a"), sum("scotch.data.either.Either", var("a"), var("b")));
    }

    @TypeParameters
    public static List<Type> parameters() {
        return asList(var("a"), var("b"));
    }

    @Value(memberName = "Right")
    public static <A, B> Applicable<B, Either<A, B>> right() {
        return applicable(value -> callable(() -> new Right<>(value)));
    }

    @ValueType(forMember = "Right")
    public static Type right$type() {
        return fn(var("b"), sum("scotch.data.either.Either", var("a"), var("b")));
    }

    private Either() {
        // intentionally empty
    }

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    public abstract <C> Either<A, C> map(Applicable<B, C> function);

    @Override
    public abstract String toString();

    @DataConstructor(ordinal = 0, memberName = "Left", dataType="Either")
    public static class Left<A, B> extends Either<A, B> {

        private final Callable<A> value;

        public Left(Callable<A> value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof Left && Objects.equals(value.call(), ((Left) o).value.call());
        }

        @Override
        public int hashCode() {
            return Objects.hash(value.call());
        }

        @SuppressWarnings("unchecked")
        @Override
        public <C> Either<A, C> map(Applicable<B, C> function) {
            return (Either<A, C>) this;
        }

        @Override
        public String toString() {
            return "Left(" + value.call() + ")";
        }
    }

    @DataConstructor(ordinal = 1, memberName = "Right", dataType = "Either")
    public static class Right<A, B> extends Either<A, B> {

        private final Callable<B> value;

        public Right(Callable<B> value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof Right && Objects.equals(value.call(), ((Right) o).value.call());
        }

        @Override
        public int hashCode() {
            return Objects.hash(value.call());
        }

        @Override
        public <C> Either<A, C> map(Applicable<B, C> function) {
            return new Right<>(function.apply(value));
        }

        @Override
        public String toString() {
            return "Right(" + value.call() + ")";
        }
    }
}
