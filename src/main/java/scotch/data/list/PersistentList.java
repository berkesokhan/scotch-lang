package scotch.data.list;

import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyIterator;
import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.Type.sum;
import static scotch.compiler.syntax.Type.var;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import scotch.compiler.syntax.Type;
import scotch.lang.DataUnion;
import scotch.lang.TypeInfo;

@DataUnion(name = "scotch.data.list.List")
public abstract class PersistentList<E> implements Iterable<E> {

    @SuppressWarnings("unchecked")
    public static <E> PersistentList<E> empty() {
        return EMPTY;
    }

    @SafeVarargs
    public static <E> PersistentList<E> listOf(E... values) {
        PersistentList<E> list = empty();
        for (int i = values.length - 1; i >= 0; i--) {
            list = list.cons(values[i]);
        }
        return list;
    }

    @TypeInfo
    public static Type type() {
        return typeOf(var("a"));
    }

    public static Type typeOf(Type argument) {
        return sum("scotch.data.list.List", asList(argument));
    }

    private static final PersistentList EMPTY = new Empty<>();

    public abstract <R> R accept(ListVisitor<E, R> visitor);

    public PersistentList<E> cons(E head) {
        return new Cons<>(head, this);
    }

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    public abstract boolean isEmpty();

    public Stream<E> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    @Override
    public String toString() {
        return "[" + join(", ", stream().map(Object::toString).collect(toList())) + "]";
    }

    public interface ListVisitor<E, R> {

        R visit(Empty<E> empty);

        R visit(Cons<E> cons);
    }

    @Data(constructor = "scotch.data.list.ListNode", enclosedBy = "scotch.data.list.List")
    public static class Cons<E> extends PersistentList<E> {

        private final E                 head;
        private final PersistentList<E> tail;

        private Cons(E head, PersistentList<E> tail) {
            this.head = head;

            this.tail = tail;
        }

        @Override
        public <R> R accept(ListVisitor<E, R> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof Cons) {
                Cons other = (Cons) o;
                return Objects.equals(head, other.head)
                    && Objects.equals(tail, other.tail);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(head, tail);
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Iterator<E> iterator() {
            return new PersistentListIterator<>(this);
        }
    }

    @Data(constructor = "scotch.data.list.EmptyList", enclosedBy = "scotch.data.list.List")
    public static class Empty<E> extends PersistentList<E> {

        private Empty() {
            // intentionally empty
        }

        @Override
        public <R> R accept(ListVisitor<E, R> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            return o == this;
        }

        @Override
        public int hashCode() {
            return Objects.hash();
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Iterator<E> iterator() {
            return emptyIterator();
        }
    }

    private static final class PersistentListIterator<E> implements Iterator<E> {

        private PersistentList<E> current;

        public PersistentListIterator(PersistentList<E> current) {
            this.current = current;
        }

        @Override
        public boolean hasNext() {
            return !current.isEmpty();
        }

        @Override
        public E next() {
            return current.accept(new ListVisitor<E, E>() {
                @Override
                public E visit(Empty<E> empty) {
                    throw new NoSuchElementException();
                }

                @Override
                public E visit(Cons<E> cons) {
                    E head = cons.head;
                    current = cons.tail;
                    return head;
                }
            });
        }
    }
}
