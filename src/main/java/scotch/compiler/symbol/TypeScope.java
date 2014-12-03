package scotch.compiler.symbol;

import java.util.Set;
import scotch.compiler.symbol.Type.VariableType;

public interface TypeScope {

    void bind(VariableType variableType, Type targetType);

    void extendContext(Type type, Set<Symbol> additionalContext);

    Type generate(Type type);

    Set<Symbol> getContext(Type type);

    Type getTarget(Type type);

    boolean isBound(VariableType variableType);
}
