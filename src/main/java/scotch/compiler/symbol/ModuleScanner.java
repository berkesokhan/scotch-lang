package scotch.compiler.symbol;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static me.qmx.jitescript.util.CodegenUtils.p;
import static scotch.compiler.symbol.DataFieldDescriptor.field;
import static scotch.compiler.symbol.Operator.operator;
import static scotch.compiler.symbol.Symbol.qualified;
import static scotch.compiler.symbol.Symbol.symbol;
import static scotch.compiler.symbol.TypeClassDescriptor.typeClass;
import static scotch.compiler.symbol.TypeInstanceDescriptor.typeInstance;
import static scotch.compiler.symbol.Value.Fixity.NONE;
import static scotch.compiler.symbol.type.Types.var;
import static scotch.compiler.util.Pair.pair;
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
import scotch.compiler.symbol.exception.IncompleteDataTypeError;
import scotch.compiler.symbol.exception.IncompleteTypeInstanceError;
import scotch.compiler.symbol.exception.InvalidMethodSignatureError;
import scotch.compiler.symbol.exception.SymbolResolutionError;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.util.Pair;

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

    public Pair<Set<SymbolEntry>, Set<TypeInstanceDescriptor>> scan() {
        classes.forEach(this::processDataTypes);
        classes.forEach(this::processDataConstructors);
        classes.forEach(clazz -> {
            processTypeClasses(clazz);
            processTypeInstances(clazz);
            processValues(clazz);
        });
        return pair(
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

    private IncompleteDataTypeError incompleteDataType(Class<?> clazz, Class<? extends Annotation> missingAnnotation) {
        return new IncompleteDataTypeError("Data type definition for class " + quote(clazz) + " in module "
            + quote(moduleName) + " is incomplete: missing method annotated with " + pp(missingAnnotation));
    }

    private IncompleteTypeInstanceError incompleteTypeInstance(TypeInstance typeInstance, Class<? extends Annotation> missingAnnotation) {
        return new IncompleteTypeInstanceError("Type instance definition for class " + quote(typeInstance.typeClass())
            + " in module " + quote(moduleName) + " is incomplete"
            + ": missing method annotated with " + pp(missingAnnotation));
    }

    @SuppressWarnings("unchecked")
    private <T> T invoke(Method method) {
        try {
            return (T) method.invoke(null);
        } catch (ReflectiveOperationException exception) {
            throw new SymbolResolutionError(exception);
        }
    }

    private String pp(Class<?> clazz) {
        return clazz.getCanonicalName();
    }

    private String pp(Method method) {
        return pp(method.getDeclaringClass()) + "#" + method.getName();
    }

    private void processDataConstructors(Class<?> clazz) {
        Optional.ofNullable(clazz.getAnnotation(DataConstructor.class)).ifPresent(annotation -> {
            DataConstructorDescriptor.Builder constructor = DataConstructorDescriptor.builder(
                qualify(annotation.dataType()),
                qualify(annotation.memberName())
            );

            Map<String, Type> fieldTypes = stream(clazz.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(DataFieldType.class))
                .map(method -> pair(method.getAnnotation(DataFieldType.class).forMember(), method))
                .collect(
                    HashMap::new,
                    (map, pair) -> pair.into((name, method) -> {
                        map.put(name, invoke(method));
                        return map;
                    }),
                    HashMap::putAll
                );

            stream(clazz.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(DataField.class))
                .map(method -> method.getAnnotation(DataField.class))
                .forEach(field -> constructor.addField(field(
                    field.memberName(),
                    Optional.ofNullable(fieldTypes.get(field.memberName()))
                        .orElseThrow(() -> incompleteDataType(clazz, DataFieldType.class))
                )));

            Symbol dataType = qualify(annotation.dataType());
            getBuilder(dataType).dataType()
                .addConstructor(constructor.build());
        });
    }

    private void processDataTypes(Class<?> clazz) {
        Optional.ofNullable(clazz.getAnnotation(DataType.class)).ifPresent(annotation -> {
            Symbol symbol = qualify(annotation.memberName());
            DataTypeDescriptor.Builder builder = getBuilder(symbol).dataType();
            Method parametersGetter = findMethod(clazz, TypeParameters.class).orElseThrow(() -> incompleteDataType(clazz, TypeParameters.class));
            validateParametersGetter(parametersGetter);
            List<Type> parametersList = invoke(parametersGetter);
            for (int i = 0; i < annotation.parameters().length; i++) {
                builder.addParameter(parametersList.get(i));
            }
            builder.withClassName(p(clazz));
        });
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

    private void processTypeInstances(Class<?> clazz) {
        Optional.ofNullable(clazz.getAnnotation(TypeInstance.class)).ifPresent(typeInstance -> {
            Method parametersGetter = findMethod(clazz, TypeParameters.class).orElseThrow(() -> incompleteTypeInstance(typeInstance, TypeParameters.class));
            Method instanceGetter = findMethod(clazz, InstanceGetter.class).orElseThrow(() -> incompleteTypeInstance(typeInstance, InstanceGetter.class));
            validateParametersGetter(parametersGetter);
            typeInstances.add(typeInstance(
                moduleName,
                symbol(typeInstance.typeClass()),
                invoke(parametersGetter),
                MethodSignature.fromMethod(instanceGetter)
            ));
        });
    }

    private void processValues(Class<?> clazz) {
        stream(clazz.getDeclaredMethods()).forEach(method -> {
            Optional.ofNullable(method.getAnnotation(Value.class)).ifPresent(value -> {
                ImmutableEntryBuilder builder = getBuilder(value.memberName());
                builder.withValueMethod(MethodSignature.fromMethod(method));
                if (value.fixity() != NONE && value.precedence() != -1) {
                    builder.withOperator(operator(value.fixity(), value.precedence()));
                }
            });
            Optional.ofNullable(method.getAnnotation(ValueType.class)).ifPresent(valueType -> {
                ImmutableEntryBuilder builder = getBuilder(valueType.forMember());
                if (Type.class.isAssignableFrom(method.getReturnType())) {
                    try {
                        builder.withValueType((Type) method.invoke(null));
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
