package scotch.data.show;

import static java.util.Arrays.asList;
import static scotch.symbol.type.Types.fn;
import static scotch.symbol.type.Types.sum;
import static scotch.symbol.type.Types.var;
import static scotch.runtime.RuntimeSupport.applicable;
import static scotch.runtime.RuntimeSupport.flatCallable;

import scotch.symbol.Member;
import scotch.symbol.TypeClass;
import scotch.symbol.TypeParameter;
import scotch.symbol.Value;
import scotch.symbol.ValueType;
import scotch.symbol.type.Type;
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
