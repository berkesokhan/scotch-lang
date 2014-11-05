package scotch.compiler.syntax;

import scotch.compiler.syntax.Type.VariableType;

public interface TypeScope {

    void bind(VariableType variableType, Type targetType);

    Type generate(Type type);

    Type getTarget(Type type);

    boolean isBound(VariableType variableType);
}
