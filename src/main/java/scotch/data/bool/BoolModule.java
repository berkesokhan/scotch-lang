package scotch.data.bool;

import static scotch.symbol.type.Types.fn;
import static scotch.symbol.type.Types.sum;
import static scotch.runtime.RuntimeSupport.applicable;
import static scotch.runtime.RuntimeSupport.callable;

import scotch.symbol.type.Type;
import scotch.symbol.Value;
import scotch.symbol.ValueType;
import scotch.symbol.type.Types;
import scotch.runtime.Applicable;

@SuppressWarnings("unused")
public final class BoolModule {

    @Value(memberName = "not")
    public static Applicable<Boolean, Boolean> not() {
        return applicable(operand -> callable(() -> !operand.call()));
    }

    @ValueType(forMember = "not")
    public static Type not$type() {
        Type boolType = Types.sum("scotch.data.bool.Bool");
        return fn(boolType, boolType);
    }

    private BoolModule() {
        // intentionally empty
    }
}
