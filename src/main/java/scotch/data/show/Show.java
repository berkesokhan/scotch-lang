package scotch.data.show;

import static java.util.Arrays.asList;
import static scotch.compiler.symbol.Type.fn;
import static scotch.compiler.symbol.Type.sum;
import static scotch.compiler.symbol.Type.var;
import static scotch.runtime.RuntimeUtil.applicable;
import static scotch.runtime.RuntimeUtil.flatCallable;

import scotch.compiler.symbol.Member;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.TypeClass;
import scotch.compiler.symbol.TypeParameter;
import scotch.compiler.symbol.Value;
import scotch.compiler.symbol.ValueType;
import scotch.runtime.Applicable;
import scotch.runtime.Callable;

@SuppressWarnings("unused")
@TypeClass(memberName = "Show", parameters = {
    @TypeParameter(name = "a"),
})
public interface Show<A> {

    @Value(memberName = "show")
    static <A> Applicable<Show<A>, Applicable<A, String>> show() {
        return applicable(instance -> applicable(operand -> flatCallable(() -> instance.call().show(operand))));
    }

    @ValueType(forMember = "show")
    static Type show$type() {
        return fn(var("a", asList("scotch.data.show.Show")), sum("scotch.data.string.String"));
    }

    @Member("show")
    Callable<String> show(Callable<A> operand);
}
