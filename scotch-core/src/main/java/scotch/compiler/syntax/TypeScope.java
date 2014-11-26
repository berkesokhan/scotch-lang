package scotch.compiler.syntax;

import java.util.Set;
import scotch.compiler.syntax.Type.VariableType;

public interface TypeScope {

    void bind(VariableType variableType, Type targetType);

    void extendContext(Type type, Set<Symbol> additionalContext);

    Type generate(Type type);

    Set<Symbol> getContext(Type type);

    Type getTarget(Type type);

    boolean isBound(VariableType variableType);
}
