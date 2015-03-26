package scotch.data.ord;

import static java.util.Arrays.asList;
import static scotch.runtime.RuntimeSupport.applicable;
import static scotch.runtime.RuntimeSupport.callable;
import static scotch.runtime.RuntimeSupport.flatCallable;
import static scotch.symbol.Value.Fixity.LEFT_INFIX;
import static scotch.symbol.type.Types.fn;
import static scotch.symbol.type.Types.var;

import scotch.data.bool.Bool;
import scotch.data.eq.Eq;
import scotch.runtime.Applicable;
import scotch.runtime.Callable;
import scotch.symbol.TypeClass;
import scotch.symbol.TypeParameter;
import scotch.symbol.Value;
import scotch.symbol.ValueType;
import scotch.symbol.type.Type;

@SuppressWarnings("unused")
@TypeClass(memberName = "Ord", parameters = {
    @TypeParameter(name = "a", constraints = {
        "scotch.data.eq.Eq",
    }),
})
public interface Ord<A> {

    static final Type a = var("a", asList("scotch.data.ord.Ord", "scotch.data.eq.Eq"));

    @Value(memberName = "compare")
    public static <A> Applicable<Eq<A>, Applicable<Ord<A>, Applicable<A, Applicable<A, Ordering>>>> compare() {
        return applicable(eq -> applicable(ord -> applicable(left -> applicable(right -> flatCallable(() -> ord.call().compare(eq, left, right))))));
    }

    @ValueType(forMember = "compare")
    public static Type compare$type() {
        return fn(a, fn(a, Ordering.TYPE));
    }

    @Value(memberName = ">", fixity = LEFT_INFIX, precedence = 5)
    public static <A> Applicable<Eq<A>, Applicable<Ord<A>, Applicable<A, Applicable<A, Boolean>>>> greaterThan() {
        return applicable(eq -> applicable(ord -> applicable(left -> applicable(right -> flatCallable(() -> ord.call().greaterThan(eq, left, right))))));
    }

    @ValueType(forMember = ">")
    public static Type greaterThan$type() {
        return fn(a, fn(a, Bool.TYPE));
    }

    @Value(memberName = ">=", fixity = LEFT_INFIX, precedence = 5)
    public static <A> Applicable<Eq<A>, Applicable<Ord<A>, Applicable<A, Applicable<A, Boolean>>>> greaterThanEquals() {
        return applicable(eq -> applicable(ord -> applicable(left -> applicable(right -> flatCallable(() -> ord.call().greaterThanEquals(eq, left, right))))));
    }

    @ValueType(forMember = ">=")
    public static Type greaterThanEquals$type() {
        return fn(a, fn(a, Bool.TYPE));
    }

    @Value(memberName = "<", fixity = LEFT_INFIX, precedence = 5)
    public static <A> Applicable<Eq<A>, Applicable<Ord<A>, Applicable<A, Applicable<A, Boolean>>>> lessThan() {
        return applicable(eq -> applicable(ord -> applicable(left -> applicable(right -> flatCallable(() -> ord.call().lessThan(eq, left, right))))));
    }

    @ValueType(forMember = "<")
    public static Type lessThan$type() {
        return fn(a, fn(a, Bool.TYPE));
    }

    @Value(memberName = "<=", fixity = LEFT_INFIX, precedence = 5)
    public static <A> Applicable<Eq<A>, Applicable<Ord<A>, Applicable<A, Applicable<A, Boolean>>>> lessThanEquals() {
        return applicable(eq -> applicable(ord -> applicable(left -> applicable(right -> flatCallable(() -> ord.call().lessThanEquals(eq, left, right))))));
    }

    @ValueType(forMember = "<=")
    public static Type lessThanEquals$type() {
        return fn(a, fn(a, Bool.TYPE));
    }

    @Value(memberName = "max")
    public static <A> Applicable<Eq<A>, Applicable<Ord<A>, Applicable<A, Applicable<A, A>>>> max() {
        return applicable(eq -> applicable(ord -> applicable(left -> applicable(right -> flatCallable(() -> ord.call().max(eq, left, right))))));
    }

    @ValueType(forMember = "max")
    public static Type max$type() {
        return fn(a, fn(a, a));
    }

    @Value(memberName = "min")
    public static <A> Applicable<Eq<A>, Applicable<Ord<A>, Applicable<A, Applicable<A, A>>>> min() {
        return applicable(eq -> applicable(ord -> applicable(left -> applicable(right -> flatCallable(() -> ord.call().min(eq, left, right))))));
    }

    @ValueType(forMember = "min")
    public static Type min$type() {
        return fn(a, fn(a, a));
    }

    default Callable<Ordering> compare(Callable<Eq<A>> eq, Callable<A> left, Callable<A> right) {
        return flatCallable(() -> {
            if (eq.call().eq(left, right).call()) {
                return Ordering.equalTo();
            } else if (lessThanEquals(eq, left, right).call()) {
                return Ordering.lessThan();
            } else {
                return Ordering.greaterThan();
            }
        });
    }

    default Callable<Boolean> greaterThan(Callable<Eq<A>> eq, Callable<A> left, Callable<A> right) {
        return callable(() -> compare(eq, left, right).call() == Ordering.greaterThan().call());
    }

    default Callable<Boolean> greaterThanEquals(Callable<Eq<A>> eq, Callable<A> left, Callable<A> right) {
        return callable(() -> compare(eq, left, right).call() != Ordering.lessThan().call());
    }

    default Callable<Boolean> lessThan(Callable<Eq<A>> eq, Callable<A> left, Callable<A> right) {
        return callable(() -> compare(eq, left, right).call() == Ordering.lessThan().call());
    }

    default Callable<Boolean> lessThanEquals(Callable<Eq<A>> eq, Callable<A> left, Callable<A> right) {
        return callable(() -> compare(eq, left, right).call() != Ordering.greaterThan().call());
    }

    default Callable<A> max(Callable<Eq<A>> eq, Callable<A> left, Callable<A> right) {
        return flatCallable(() -> lessThanEquals(eq, left, right).call() ? right : left);
    }

    default Callable<A> min(Callable<Eq<A>> eq, Callable<A> left, Callable<A> right) {
        return flatCallable(() -> lessThanEquals(eq, left, right).call() ? left : right);
    }
}
