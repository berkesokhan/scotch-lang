package scotch.data.list;

import static java.util.Arrays.asList;
import static scotch.data.list.PersistentList.typeOf;
import static scotch.lang.Type.var;

import java.util.List;
import java.util.function.Function;
import scotch.data.eq.EqInstance;
import scotch.lang.Type;
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
        return asList(typeOf(var("a", asList("scotch.data.eq.Eq"))));
    }

    @TypeMember(name = "/=")
    public Function<PersistentList<E>, Boolean> notEquals(PersistentList<E> left) {
        return right -> !equals(left).apply(right);
    }
}
