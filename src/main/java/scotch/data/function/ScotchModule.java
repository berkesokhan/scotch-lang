package scotch.data.function;

import static scotch.symbol.Value.Fixity.RIGHT_INFIX;
import static scotch.symbol.type.Types.fn;
import static scotch.symbol.type.Types.var;
import static scotch.runtime.RuntimeUtil.applicable;
import static scotch.runtime.RuntimeUtil.callable;
import static scotch.runtime.RuntimeUtil.flatCallable;

import scotch.symbol.Value;
import scotch.symbol.ValueType;
import scotch.symbol.type.Type;
import scotch.runtime.Applicable;
import scotch.runtime.Callable;

@SuppressWarnings("unused")
public class ScotchModule {

    @SuppressWarnings("unchecked")
    @Value(memberName = ".", fixity = RIGHT_INFIX, precedence = 9)
    public static <A, B, C> Applicable compose() {
        return applicable(
            fnBC -> applicable(
                fnAB -> applicable(
                    a -> callable(
                        () -> ((Applicable) fnBC.call()).apply(((Applicable) fnAB.call()).apply((Callable) a))))));
    }

    @ValueType(forMember = ".")
    public static Type compose$type() {
        return fn(fn(var("b"), var("c")), fn(fn(var("a"), var("b")), fn(var("a"), var("c"))));
    }

    @Value(memberName = "$", fixity = RIGHT_INFIX, precedence = 0)
    public static <A, B> Applicable<Applicable<A, B>, Applicable<A, B>> dollarSign() {
        return applicable(function -> applicable(argument -> flatCallable(() -> function.call().apply(argument))));
    }

    @ValueType(forMember = "$")
    public static Type dollarSign$type() {
        return fn(fn(var("a"), var("b")), fn(var("a"), var("b")));
    }
}
