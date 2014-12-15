package scotch.compiler.symbol;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static scotch.compiler.symbol.Operator.operator;
import static scotch.compiler.symbol.Symbol.fromString;
import static scotch.compiler.symbol.Symbol.qualified;
import static scotch.compiler.symbol.Type.var;
import static scotch.compiler.symbol.TypeClassDescriptor.typeClass;
import static scotch.compiler.symbol.TypeInstanceDescriptor.typeInstance;
import static scotch.compiler.symbol.Value.Fixity.NONE;
import static scotch.data.tuple.TupleValues.tuple2;
import static scotch.util.StringUtil.quote;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.google.common.collect.ImmutableSet;
import scotch.compiler.symbol.SymbolEntry.ImmutableEntryBuilder;
import scotch.compiler.symbol.exception.IncompleteTypeInstanceError;
import scotch.compiler.symbol.exception.InvalidMethodSignatureError;
import scotch.compiler.symbol.exception.SymbolResolutionError;
import scotch.data.tuple.Tuple2;

public class ModuleScanner {

    private final String                             moduleName;
    private final List<Class<?>>                     classes;
    private final Map<Symbol, ImmutableEntryBuilder> builders;
    private final Set<TypeInstanceDescriptor>        typeInstances;

    public ModuleScanner(String moduleName, List<Class<?>> classes) {
        this.moduleName = moduleName;
        this.classes = classes;
        this.builders = new HashMap<>();
        this.typeInstances = new HashSet<>();
    }

    public Tuple2<Set<SymbolEntry>, Set<TypeInstanceDescriptor>> scan() {
        classes.forEach(clazz -> {
            processValues(clazz);
            processTypeClasses(clazz);
            processTypeInstances(clazz);
        });
        return tuple2(
            builders.values().stream().map(ImmutableEntryBuilder::build).collect(toSet()),
            ImmutableSet.copyOf(typeInstances)
        );
    }

    private List<Symbol> computeMembers(Class<?> clazz) {
        return stream(clazz.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(Member.class))
            .map(method -> method.getAnnotation(Member.class))
            .map(Member::value)
            .map(member -> qualified(moduleName, member))
            .collect(toList());
    }

    private List<Type> computeParameters(TypeClass typeClass) {
        return stream(typeClass.parameters())
            .map(parameter -> var(parameter.name(), asList(parameter.constraints())))
            .collect(toList());
    }

    private Optional<Method> findMethod(Class<?> clazz, Class<? extends Annotation> annotation) {
        return stream(clazz.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(annotation))
            .findFirst();
    }

    private ImmutableEntryBuilder getBuilder(String memberName) {
        return builders.computeIfAbsent(qualify(memberName), SymbolEntry::immutableEntry);
    }

    private ImmutableEntryBuilder getBuilder(Symbol memberSymbol) {
        return builders.computeIfAbsent(memberSymbol, SymbolEntry::immutableEntry);
    }

    private IncompleteTypeInstanceError incompleteTypeInstance(TypeInstance typeInstance, Class<? extends Annotation> missingAnnotation) {
        return new IncompleteTypeInstanceError("Type instance definition for class " + quote(typeInstance.typeClass())
            + " in module " + quote(moduleName) + " is incomplete"
            + ": missing method annotated with " + pp(missingAnnotation));
    }

    private String pp(Class<?> clazz) {
        return clazz.getCanonicalName();
    }

    private String pp(Method method) {
        return pp(method.getDeclaringClass()) + "#" + method.getName();
    }

    private void processTypeClasses(Class<?> clazz) {
        Optional.ofNullable(clazz.getAnnotation(TypeClass.class)).ifPresent(typeClass -> {
            ImmutableEntryBuilder builder = getBuilder(typeClass.memberName());
            Symbol symbol = qualify(typeClass.memberName());
            List<Symbol> members = computeMembers(clazz);
            builder.withTypeClass(typeClass(symbol, computeParameters(typeClass), members));
            members.forEach(member -> getBuilder(member).withMemberOf(symbol));
        });
    }

    @SuppressWarnings("unchecked")
    private void processTypeInstances(Class<?> clazz) {
        Optional.ofNullable(clazz.getAnnotation(TypeInstance.class)).ifPresent(typeInstance -> {
            Method parametersGetter = findMethod(clazz, TypeParameters.class).orElseThrow(() -> incompleteTypeInstance(typeInstance, TypeParameters.class));
            Method instanceGetter = findMethod(clazz, InstanceGetter.class).orElseThrow(() -> incompleteTypeInstance(typeInstance, InstanceGetter.class));
            validateParametersGetter(parametersGetter);
            try {
                typeInstances.add(typeInstance(
                    moduleName,
                    fromString(typeInstance.typeClass()),
                    (List<Type>) parametersGetter.invoke(null),
                    MethodSignature.fromMethod(instanceGetter)
                ));
            } catch (ReflectiveOperationException exception) {
                throw new SymbolResolutionError(exception);
            }
        });
    }

    private void processValues(Class<?> clazz) {
        stream(clazz.getDeclaredMethods()).forEach(method -> {
            Optional.ofNullable(method.getAnnotation(Value.class)).ifPresent(value -> {
                ImmutableEntryBuilder builder = getBuilder(value.memberName());
                builder.withValueSignature(MethodSignature.fromMethod(method));
                if (value.fixity() != NONE && value.precedence() != -1) {
                    builder.withOperator(operator(value.fixity(), value.precedence()));
                }
            });
            Optional.ofNullable(method.getAnnotation(ValueType.class)).ifPresent(valueType -> {
                ImmutableEntryBuilder builder = getBuilder(valueType.forMember());
                if (Type.class.isAssignableFrom(method.getReturnType())) {
                    try {
                        builder.withValue((Type) method.invoke(null));
                    } catch (ReflectiveOperationException exception) {
                        throw new SymbolResolutionError(exception);
                    }
                } else {
                    throw new InvalidMethodSignatureError("Method " + pp(method)
                        + " annotated by " + pp(ValueType.class)
                        + " does not return " + pp(Type.class));
                }
            });
        });
    }

    private Symbol qualify(String memberName) {
        return qualified(moduleName, memberName);
    }

    private void validateParametersGetter(Method parametersGetter) {
        ParameterizedType returnType = (ParameterizedType) parametersGetter.getGenericReturnType();
        if (!List.class.isAssignableFrom((Class<?>) returnType.getRawType()) || !Type.class.isAssignableFrom((Class<?>) returnType.getActualTypeArguments()[0])) {
            throw new InvalidMethodSignatureError("Method " + pp(parametersGetter)
                + " annotated by " + pp(TypeParameters.class)
                + " does not return " + pp(List.class) + "<" + pp(Type.class) + ">");
        } else if (parametersGetter.getParameterCount() != 0) {
            throw new InvalidMethodSignatureError("Method " + pp(parametersGetter)
                + " annotated by " + pp(TypeParameters.class)
                + " should not accept arguments");
        }
    }
}
