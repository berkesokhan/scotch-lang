package scotch.data.either;

import static java.util.Arrays.asList;
import static scotch.compiler.symbol.type.Types.fn;
import static scotch.compiler.symbol.type.Types.sum;
import static scotch.compiler.symbol.type.Types.var;
import static scotch.runtime.RuntimeUtil.applicable;
import static scotch.runtime.RuntimeUtil.callable;

import java.util.List;
import java.util.Objects;
import scotch.compiler.symbol.DataConstructor;
import scotch.compiler.symbol.DataType;
import scotch.compiler.symbol.TypeParameter;
import scotch.compiler.symbol.TypeParameters;
import scotch.compiler.symbol.Value;
import scotch.compiler.symbol.ValueType;
import scotch.compiler.symbol.type.Type;
import scotch.runtime.Applicable;
import scotch.runtime.Callable;

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

    @DataConstructor(memberName = "Left", dataType="Either", parameters = {
        @TypeParameter(name = "a"),
    })
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

    @DataConstructor(memberName = "Right", dataType = "Either", parameters = {
        @TypeParameter(name = "b"),
    })
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
