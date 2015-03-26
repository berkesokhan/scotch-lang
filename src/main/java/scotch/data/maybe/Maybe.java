package scotch.data.maybe;

import static java.util.Arrays.asList;
import static scotch.runtime.RuntimeSupport.applicable;
import static scotch.runtime.RuntimeSupport.callable;
import static scotch.runtime.RuntimeSupport.flatCallable;
import static scotch.symbol.type.Types.fn;
import static scotch.symbol.type.Types.sum;
import static scotch.symbol.type.Types.var;

import java.util.List;
import java.util.Objects;
import scotch.runtime.Applicable;
import scotch.runtime.Callable;
import scotch.runtime.RuntimeSupport;
import scotch.symbol.DataConstructor;
import scotch.symbol.DataField;
import scotch.symbol.DataFieldType;
import scotch.symbol.DataType;
import scotch.symbol.TypeParameter;
import scotch.symbol.TypeParameters;
import scotch.symbol.Value;
import scotch.symbol.ValueType;
import scotch.symbol.type.Type;

@SuppressWarnings("unused")
@DataType(memberName = "Maybe", parameters = {
    @TypeParameter(name = "a"),
})
public abstract class Maybe<A> {

    public static final  Type            TYPE    = sum("scotch.data.maybe.Maybe", var("a"));
    private static final Callable<Maybe> NOTHING = callable(Nothing::new);

    @Value(memberName = "Just")
    public static <A> Applicable<A, Maybe<A>> just() {
        return applicable(value -> callable(() -> new Just<>(value)));
    }

    @SuppressWarnings("unchecked")
    public static <A> Maybe<A> just(A value) {
        return (Maybe<A>) just().apply(RuntimeSupport.box(value)).call();
    }

    @ValueType(forMember = "Just")
    public static Type just$type() {
        return fn(var("a"), TYPE);
    }

    @SuppressWarnings("unchecked")
    @Value(memberName = "Nothing")
    public static <A> Callable<Maybe<A>> nothing() {
        return (Callable) NOTHING;
    }

    @ValueType(forMember = "Nothing")
    public static Type nothing$type() {
        return TYPE;
    }

    @TypeParameters
    public static List<Type> parameters() {
        return asList(var("a"));
    }

    private Maybe() {
        // intentionally empty
    }

    public abstract boolean equals(Object o);

    public abstract int hashCode();

    public abstract <B> Callable<Maybe<B>> map(Applicable<A, Maybe<B>> function);

    public abstract String toString();

    @DataConstructor(ordinal = 0, memberName = "Nothing", dataType = "Maybe")
    public static class Nothing<A> extends Maybe<A> {

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof Nothing;
        }

        @Override
        public int hashCode() {
            return Objects.hash(17);
        }

        @Override
        public <B> Callable<Maybe<B>> map(Applicable<A, Maybe<B>> function) {
            return nothing();
        }

        @Override
        public String toString() {
            return "Nothing";
        }
    }

    @DataConstructor(ordinal = 1, memberName = "Just", dataType = "Maybe")
    public static class Just<A> extends Maybe<A> {

        @DataFieldType(forMember = "value")
        public static Type value$type() {
            return var("a");
        }

        private final Callable<A> value;

        private Just(Callable<A> value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof Just && Objects.equals(value.call(), ((Just) o).value.call());
        }

        @DataField(memberName = "value", ordinal = 0)
        public Callable<A> getValue() {
            return value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public <B> Callable<Maybe<B>> map(Applicable<A, Maybe<B>> function) {
            return flatCallable(() -> function.apply(value));
        }

        @Override
        public String toString() {
            return "Just " + value.call() + "";
        }
    }
}
