package scotch.lang;

import java.util.List;
import scotch.compiler.syntax.Type;

public interface Instance {

    List<Type> getArguments();
}
