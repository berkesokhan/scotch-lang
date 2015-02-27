package scotch.compiler.symbol.type;

import java.util.Set;
import scotch.compiler.symbol.Symbol;

public interface TypeScope {

    Unification bind(VariableType variableType, Type targetType);

    void extendContext(Type type, Set<Symbol> additionalContext);

    void generalize(Type type);

    Type generate(Type type);

    Set<Symbol> getContext(Type type);

    Type getTarget(Type type);

    void implement(Symbol typeClass, SumType type);

    boolean isBound(VariableType variableType);

    boolean isGeneric(VariableType variableType);

    boolean isImplemented(Symbol typeClass, SumType type);

    VariableType reserveType();

    void specialize(Type type);
}
