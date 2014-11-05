package scotch.compiler.syntax;

import static scotch.compiler.syntax.Type.fn;

import java.util.HashMap;
import java.util.Map;
import scotch.compiler.syntax.Type.FunctionType;
import scotch.compiler.syntax.Type.SumType;
import scotch.compiler.syntax.Type.TypeVisitor;
import scotch.compiler.syntax.Type.VariableType;

public class DefaultTypeScope implements TypeScope {

    private final Map<Type, Type> bindings;

    public DefaultTypeScope() {
        this.bindings = new HashMap<>();
    }

    @Override
    public void bind(VariableType variableType, Type targetType) {
        if (isBound(variableType)) {
            throw new UnsupportedOperationException("Can't re-bind type " + variableType + " to new target "
                + targetType + ": current binding is " + getTarget(variableType));
        } else {
            bindings.put(variableType, targetType);
        }
    }

    @Override
    public Type generate(Type type) {
        return type.accept(new TypeVisitor<Type>() {
            @Override
            public Type visit(FunctionType type) {
                return fn(generate(type.getArgument()), generate(type.getResult()));
            }

            @Override
            public Type visit(VariableType type) {
                return getTarget(type);
            }

            @Override
            public Type visit(SumType type) {
                return type;
            }
        });
    }

    @Override
    public Type getTarget(Type type) {
        return type.accept(new TypeVisitor<Type>() {
            @Override
            public Type visit(VariableType type) {
                Type result = type;
                while (bindings.containsKey(result)) {
                    result = bindings.get(result);
                }
                return result;
            }

            @Override
            public Type visitOtherwise(Type type) {
                throw new IllegalArgumentException("Can't get target of non-variable type " + type);
            }
        });
    }

    @Override
    public boolean isBound(VariableType variableType) {
        return bindings.containsKey(variableType);
    }
}
