package scotch.function;

import static scotch.compiler.symbol.Type.fn;
import static scotch.compiler.symbol.Type.var;
import static scotch.compiler.symbol.Value.Fixity.RIGHT_INFIX;
import static scotch.runtime.RuntimeUtil.applicable;
import static scotch.runtime.RuntimeUtil.flatCallable;

import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.Value;
import scotch.compiler.symbol.ValueType;
import scotch.runtime.Applicable;

@SuppressWarnings("unused")
public class ScotchModule {

    @Value(memberName = "$", fixity = RIGHT_INFIX, precedence = 0)
    public static <A, B> Applicable<Applicable<A, B>, Applicable<A, B>> applyLeft() {
        return applicable(function -> applicable(argument -> flatCallable(() -> function.call().apply(argument))));
    }

    @ValueType(forMember = "$")
    public static Type applyLeft$type() {
        return fn(fn(var("a"), var("b")), fn(var("a"), var("b")));
    }
}
