package scotch.lang;

import java.util.List;
import scotch.compiler.ast.Type;

public interface Instance {

    List<Type> getArguments();
}
