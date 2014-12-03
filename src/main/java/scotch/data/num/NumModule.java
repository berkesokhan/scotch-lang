package scotch.data.num;

import static scotch.compiler.symbol.Type.fn;
import static scotch.compiler.symbol.Type.sum;
import static scotch.compiler.symbol.Value.Fixity.LEFT_INFIX;
import static scotch.runtime.RuntimeUtil.fn2;

import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.Value;
import scotch.compiler.symbol.ValueType;
import scotch.runtime.Applicable;

@SuppressWarnings("unused")
public class NumModule {

    @Value(memberName = "+", fixity = LEFT_INFIX, precedence = 7)
    public static Applicable<Integer, Applicable<Integer, Integer>> plus() {
        return fn2((left, right) -> left.call() + right.call());
    }

    @ValueType(forMember = "+")
    public static Type plus$type() {
        Type intType = sum("scotch.data.int.Int");
        return fn(intType, fn(intType, intType));
    }
}
