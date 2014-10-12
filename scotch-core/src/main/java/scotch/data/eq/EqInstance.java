package scotch.data.eq;

import java.util.function.Function;
import scotch.lang.Instance;
import scotch.lang.TypeInterface;
import scotch.lang.TypeMember;

@TypeInterface(className = "scotch.data.eq.Eq")
public interface EqInstance<E> extends Instance {

    @TypeMember(name = "==")
    default Function<E, Boolean> eq(E left) {
        return right -> !notEq(left).apply(right);
    }

    @TypeMember(name = "/=")
    default Function<E, Boolean> notEq(E left) {
        return right -> !eq(left).apply(right);
    }
}
