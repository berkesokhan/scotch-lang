package scotch.compiler.symbol;

import java.util.Set;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.symbol.type.VariableType;

public interface TypeScope {

    Unification bind(VariableType variableType, Type targetType);

    void extendContext(Type type, Set<Symbol> additionalContext);

    void generalize(Type type);

    Type generate(Type type);

    Type genericCopy(Type type);

    Set<Symbol> getContext(Type type);

    Type getTarget(Type type);

    boolean isBound(VariableType variableType);

    Type reserveType();

    void specialize(Type type);
}
