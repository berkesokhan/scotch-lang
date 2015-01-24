package scotch.data.bool;

import static scotch.compiler.symbol.type.Type.fn;
import static scotch.compiler.symbol.type.Type.sum;
import static scotch.runtime.RuntimeUtil.applicable;
import static scotch.runtime.RuntimeUtil.callable;

import scotch.compiler.symbol.type.Type;
import scotch.compiler.symbol.Value;
import scotch.compiler.symbol.ValueType;
import scotch.runtime.Applicable;

@SuppressWarnings("unused")
public final class BoolModule {

    @Value(memberName = "not")
    public static Applicable<Boolean, Boolean> not() {
        return applicable(operand -> callable(() -> !operand.call()));
    }

    @ValueType(forMember = "not")
    public static Type not$type() {
        Type boolType = sum("scotch.data.bool.Bool");
        return fn(boolType, boolType);
    }

    private BoolModule() {
        // intentionally empty
    }
}
