package scotch.data.maybe;

import static java.util.Arrays.asList;
import static scotch.compiler.symbol.type.Types.fn;
import static scotch.compiler.symbol.type.Types.sum;
import static scotch.compiler.symbol.type.Types.var;
import static scotch.runtime.RuntimeUtil.applicable;
import static scotch.runtime.RuntimeUtil.callable;
import static scotch.runtime.RuntimeUtil.flatCallable;

import java.util.List;
import java.util.Objects;
import scotch.compiler.symbol.DataConstructor;
import scotch.compiler.symbol.DataField;
import scotch.compiler.symbol.DataFieldType;
import scotch.compiler.symbol.DataType;
import scotch.compiler.symbol.TypeParameter;
import scotch.compiler.symbol.TypeParameters;
import scotch.compiler.symbol.Value;
import scotch.compiler.symbol.ValueType;
import scotch.compiler.symbol.type.Type;
import scotch.runtime.Applicable;
import scotch.runtime.Callable;

@SuppressWarnings("unused")
@DataType(memberName = "Maybe", parameters = {
    @TypeParameter(name = "a"),
})
public abstract class Maybe<A> {

    public static final  Type            TYPE    = sum("scotch.data.maybe.Maybe");
    private static final Callable<Maybe> NOTHING = callable(Nothing::new);

    @Value(memberName = "Just")
    public static <A> Applicable<A, Maybe<A>> just() {
        return applicable(value -> callable(() -> new Just<>(value)));
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

    @DataConstructor(memberName = "Nothing", dataType = "Maybe")
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

    @DataConstructor(memberName = "Just", dataType = "Maybe", parameters = {
        @TypeParameter(name = "a"),
    })
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
            return o == this || o instanceof Just && Objects.equals(value, ((Just) o).value);
        }

        @DataField(memberName = "value")
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
