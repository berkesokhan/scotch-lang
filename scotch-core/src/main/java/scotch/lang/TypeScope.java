package scotch.lang;

import static java.util.stream.Collectors.toList;
import static scotch.lang.Type.ctor;
import static scotch.lang.Type.field;
import static scotch.lang.Type.fn;
import static scotch.lang.Type.lookup;
import static scotch.lang.Type.union;
import static scotch.lang.Type.var;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import scotch.lang.Type.FunctionType;
import scotch.lang.Type.MemberField;
import scotch.lang.Type.TypeVisitor;
import scotch.lang.Type.UnionLookup;
import scotch.lang.Type.UnionType;
import scotch.lang.Type.VariableType;

public class TypeScope {

    private final ContextScope            contextScope;
    private final Map<VariableType, Type> bindings;
    private int currentId = 1;

    public TypeScope(ContextScope contextScope) {
        this.contextScope = contextScope;
        this.bindings = new HashMap<>();
    }

    public Unification bind(Type genericType, List<Type> arguments) {
        return new ArgumentBinder(this, genericType, arguments).bind();
    }

    public void bind(VariableType variableType, Type targetType) {
        if (isBound(variableType)) {
            throw new UnsupportedOperationException("Can't re-bind type " + variableType + " to new target "
                + targetType + ": current binding is " + getTarget(variableType));
        } else {
            bindings.put(variableType, targetType);
        }
    }

    public boolean contextsMatch(Type target, VariableType variableType) {
        return getContext(target).containsAll(variableType.getContext());
    }

    public Type generate(Type type) {
        return type.accept(new TypeVisitor<Type>() {
            @Override
            public Type visit(FunctionType functionType) {
                return fn(generate(functionType.getArgument()), generate(functionType.getResult()));
            }

            @Override
            public Type visit(UnionLookup lookup) {
                return lookup(
                    lookup.getName(),
                    lookup.getArguments().stream()
                        .map(TypeScope.this::generate)
                        .collect(toList())
                );
            }

            @Override
            public Type visit(VariableType variableType) {
                return getTarget(variableType);
            }

            @Override
            public Type visit(UnionType unionType) {
                return union(
                    unionType.getName(),
                    unionType.getArguments().stream()
                        .map(TypeScope.this::generate)
                        .collect(toList()),
                    unionType.getMembers().stream()
                        .map(member -> ctor(
                            member.getName(),
                            member.getArguments().stream()
                                .map(TypeScope.this::generate)
                                .collect(toList()), // compiles despite IDE error
                            member.getFields().stream()
                                .map((MemberField field) -> field(field.getName(), generate(field.getType())))
                                .collect(toList()) // compiles despite IDE error
                        ))
                        .collect(toList())
                );
            }
        });
    }

    public List<String> getContext(Type type) {
        return contextScope.getContext(type);
    }

    public Type getTarget(Type type) {
        return type.accept(new TypeVisitor<Type>() {
            @SuppressWarnings("SuspiciousMethodCalls")
            @Override
            public Type visit(VariableType variableType) {
                Type result = variableType;
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

    public boolean isBound(VariableType variableType) {
        return bindings.containsKey(variableType);
    }

    public Type reserve(List<String> context) {
        return var("t" + currentId++, context);
    }
}
