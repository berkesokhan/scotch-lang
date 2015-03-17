package scotch.data.list;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static scotch.symbol.Value.Fixity.RIGHT_INFIX;
import static scotch.symbol.type.Types.fn;
import static scotch.symbol.type.Types.sum;
import static scotch.symbol.type.Types.var;
import static scotch.runtime.RuntimeSupport.applicable;
import static scotch.runtime.RuntimeSupport.callable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import scotch.symbol.DataConstructor;
import scotch.symbol.DataField;
import scotch.symbol.DataFieldType;
import scotch.symbol.DataType;
import scotch.symbol.TypeParameter;
import scotch.symbol.TypeParameters;
import scotch.symbol.Value;
import scotch.symbol.ValueType;
import scotch.symbol.type.Type;
import scotch.runtime.Applicable;
import scotch.runtime.Callable;

@SuppressWarnings("unused")
@DataType(memberName = "[]", parameters = {
    @TypeParameter(name = "a")
})
public abstract class ConsList<A> {

    private static final Callable<EmptyCell> EMPTY = callable(EmptyCell::new);

    @Value(memberName = ":", fixity = RIGHT_INFIX, precedence = 5)
    public static <A> Applicable<A, Applicable<ConsList<A>, ConsList<A>>> cons() {
        return applicable(head -> applicable(tail -> callable(() -> new ConsCell<>(head, tail))));
    }

    @ValueType(forMember = ":")
    public static Type cons$type() {
        return fn(var("a"), fn(sum("scotch.data.list.[]", var("a")), sum("scotch.data.list.[]", var("a"))));
    }

    @SuppressWarnings("unchecked")
    @Value(memberName = "[]")
    public static <A> Callable<ConsList<A>> empty() {
        return (Callable) EMPTY;
    }

    @ValueType(forMember = "[]")
    public static Type empty$type() {
        return sum("scotch.data.list.[]", var("a"));
    }

    @TypeParameters
    public static List<Type> parameters() {
        return asList(var("a"));
    }

    private ConsList() {
        // intentionally empty
    }

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    public String toString() {
        return "[" + toString_().stream().map(Object::toString).collect(joining(", ")) + "]";
    }

    protected abstract List<A> toString_();

    @AllArgsConstructor
    @DataConstructor(ordinal = 1, memberName = ":", dataType = "[]", parameters = {
        @TypeParameter(name = "a")
    })
    public static class ConsCell<A> extends ConsList<A> {

        @DataFieldType(forMember = "_0")
        public static Type head$type() {
            return var("a");
        }

        @DataFieldType(forMember = "_1")
        public static Type tail$type() {
            return sum("scotch.data.list.[]", asList(var("a")));
        }

        private final Callable<A> head;
        private final Callable<ConsList<A>> tail;

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof ConsCell) {
                ConsCell other = (ConsCell) o;
                return Objects.equals(head.call(), other.head.call())
                    && Objects.equals(tail.call(), other.tail.call());
            } else {
                return false;
            }
        }

        @DataField(ordinal = 0, memberName = "_0")
        public Callable<A> getHead() {
            return head;
        }

        @DataField(ordinal = 1, memberName = "_1")
        public Callable<ConsList<A>> getTail() {
            return tail;
        }

        @Override
        public int hashCode() {
            return Objects.hash(head.call(), tail.call());
        }

        @Override
        protected List<A> toString_() {
            return new ArrayList<A>() {{
                add(head.call());
                addAll(tail.call().toString_());
            }};
        }
    }

    @EqualsAndHashCode(callSuper = false)
    @DataConstructor(ordinal = 0, memberName = "[]", dataType = "[]")
    public static class EmptyCell<A> extends ConsList<A> {

        @Override
        protected List<A> toString_() {
            return asList();
        }
    }
}
