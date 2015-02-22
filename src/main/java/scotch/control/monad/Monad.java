package scotch.control.monad;

import static java.util.Arrays.asList;
import static scotch.compiler.symbol.Value.Fixity.LEFT_INFIX;
import static scotch.compiler.symbol.type.Types.ctor;
import static scotch.compiler.symbol.type.Types.fn;
import static scotch.compiler.symbol.type.Types.sum;
import static scotch.compiler.symbol.type.Types.var;
import static scotch.runtime.RuntimeUtil.applicable;
import static scotch.runtime.RuntimeUtil.callable;
import static scotch.runtime.RuntimeUtil.flatCallable;

import scotch.compiler.symbol.TypeClass;
import scotch.compiler.symbol.TypeParameter;
import scotch.compiler.symbol.Value;
import scotch.compiler.symbol.ValueType;
import scotch.compiler.symbol.type.Type;
import scotch.runtime.Applicable;
import scotch.runtime.Callable;

@SuppressWarnings("unused")
@TypeClass(memberName = "Monad", parameters = {
    @TypeParameter(name = "m"),
})
public interface Monad {

    static final Type m = var("m", asList("scotch.control.monad.Monad"));

    @Value(memberName = ">>=", fixity = LEFT_INFIX, precedence = 1)
    public static Applicable bind() {
        return applicable(
            instance -> applicable(
                arg -> applicable(
                    fn -> callable(
                        () -> ((Monad) instance.call()).bind(arg, (Applicable) fn)))));
    }

    @ValueType(forMember = ">>=")
    public static Type bind$type() {
        return fn(ctor(m, var("a")), fn(fn(var("a"), ctor(m, var("b"))), ctor(m, var("b"))));
    }

    @Value(memberName = "fail")
    public static Applicable fail() {
        return applicable(
            instance -> applicable(
                message -> callable(
                    () -> ((Monad) instance.call()).fail(message))));
    }

    @ValueType(forMember = "fail")
    public static Type fail$type() {
        return fn(sum("scotch.data.string.String"), ctor(m, var("a")));
    }

    @Value(memberName = ">>", fixity = LEFT_INFIX, precedence = 1)
    public static Applicable then() {
        return applicable(
            instance -> applicable(
                firstValue -> applicable(
                    nextValue -> callable(
                        () -> ((Monad) instance.call()).then(firstValue, nextValue)))));
    }

    @ValueType(forMember = ">>")
    public static Type then$type() {
        return fn(ctor(m, var("a")), fn(ctor(m, var("b")), ctor(m, var("b"))));
    }

    @Value(memberName = "return")
    public static Applicable wrap() {
        return applicable(
            instance -> applicable(
                value -> callable(
                    () -> ((Monad) instance.call()).wrap(value))));
    }

    @ValueType(forMember = "return")
    public static Type wrap$type() {
        return fn(var("a"), ctor(m, var("a")));
    }

    Callable bind(Callable value, Applicable transformer);

    Callable fail(Callable message);

    default Callable then(Callable firstValue, Callable nextValue) {
        return bind(firstValue, applicable(arg -> flatCallable(() -> nextValue)));
    }

    Callable wrap(Callable value);
}
