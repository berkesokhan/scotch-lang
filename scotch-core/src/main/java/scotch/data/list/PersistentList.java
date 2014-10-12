package scotch.data.list;

import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyIterator;
import static java.util.stream.Collectors.toList;
import static scotch.lang.Type.constant;
import static scotch.lang.Type.ctor;
import static scotch.lang.Type.field;
import static scotch.lang.Type.lookup;
import static scotch.lang.Type.union;
import static scotch.lang.Type.var;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import scotch.lang.DataUnion;
import scotch.lang.Type;
import scotch.lang.TypeInfo;

@DataUnion(name = "scotch.data.list.List")
public abstract class PersistentList<E> implements Iterable<E> {

    private static final PersistentList EMPTY = new EmptyList<>();

    @SuppressWarnings("unchecked")
    public static <E> PersistentList<E> empty() {
        return EMPTY;
    }

    @TypeInfo
    public static Type type() {
        return typeOf(var("a"));
    }

    public static Type typeOf(Type argument) {
        return union("scotch.data.list.List", asList(argument), asList(
            ctor("scotch.data.list.ListNode", asList(argument), asList(
                field("head", argument),
                field("tail", lookup("scotch.data.list.List", asList(argument)))
            )),
            constant("scotch.data.list.EmptyList")
        ));
    }

    @Override
    public abstract boolean equals(Object o);

    public abstract E getHead();

    public abstract PersistentList<E> getTail();

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

    @Data(constructor = "scotch.data.list.EmptyList", enclosedBy = "scotch.data.list.List")
    public static class EmptyList<E> extends PersistentList<E> {

        private EmptyList() {
            // intentionally empty
        }

        @Override
        public boolean equals(Object o) {
            return o == this;
        }

        @Override
        public E getHead() {
            throw new IllegalStateException();
        }

        @Override
        public PersistentList<E> getTail() {
            throw new IllegalStateException();
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

    @Data(constructor = "scotch.data.list.ListNode", enclosedBy = "scotch.data.list.List")
    public static class ListNode<E> extends PersistentList<E> {

        private final E                 head;
        private final PersistentList<E> tail;

        private ListNode(E head, PersistentList<E> tail) {
            this.head = head;

            this.tail = tail;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof ListNode) {
                ListNode other = (ListNode) o;
                return Objects.equals(head, other.head)
                    && Objects.equals(tail, other.tail);
            } else {
                return false;
            }
        }

        @Override
        public E getHead() {
            return head;
        }

        @Override
        public PersistentList<E> getTail() {
            return tail;
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
            if (hasNext()) {
                E head = current.getHead();
                current = current.getTail();
                return head;
            } else {
                throw new NoSuchElementException();
            }
        }
    }
}
