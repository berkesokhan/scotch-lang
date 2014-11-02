package scotch.data.list;

import static java.util.Arrays.asList;
import static scotch.compiler.ast.Type.var;
import static scotch.data.list.PersistentList.typeOf;

import java.util.List;
import java.util.function.Function;
import scotch.compiler.ast.Type;
import scotch.data.eq.EqInstance;
import scotch.lang.TypeInstance;
import scotch.lang.TypeMember;

@TypeInstance(className = "scotch.data.eq.Eq")
public class PersistentListEq<E> implements EqInstance<PersistentList<E>> {

    @TypeMember(name = "==")
    public Function<PersistentList<E>, Boolean> equals(PersistentList<E> left) {
        return left::equals;
    }

    @Override
    public List<Type> getArguments() {
        return asList(typeOf(var("a")));
    }

    @TypeMember(name = "/=")
    public Function<PersistentList<E>, Boolean> notEquals(PersistentList<E> left) {
        return right -> !equals(left).apply(right);
    }
}
