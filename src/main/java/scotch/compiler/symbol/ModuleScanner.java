package scotch.compiler.symbol;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static scotch.compiler.symbol.Operator.operator;
import static scotch.compiler.symbol.Symbol.fromString;
import static scotch.compiler.symbol.Symbol.qualified;
import static scotch.compiler.symbol.Type.var;
import static scotch.compiler.symbol.TypeClassDescriptor.typeClass;
import static scotch.compiler.symbol.TypeInstanceDescriptor.typeInstance;
import static scotch.compiler.symbol.Value.Fixity.NONE;
import static scotch.data.tuple.TupleValues.tuple2;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.google.common.collect.ImmutableSet;
import scotch.compiler.symbol.SymbolEntry.ImmutableEntryBuilder;
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

    @SuppressWarnings("unchecked")
    private void processTypeInstances(Class<?> clazz) {
        Optional.ofNullable(clazz.getAnnotation(TypeInstance.class)).ifPresent(typeInstance -> {
            Method parametersGetter = findMethod(clazz, TypeParameters.class).orElseThrow(() -> new IncompleteTypeInstanceError(""));
            Method instanceGetter = findMethod(clazz, InstanceGetter.class).orElseThrow(() -> new IncompleteTypeInstanceError(""));
            try {
                typeInstances.add(typeInstance(
                    moduleName,
                    fromString(typeInstance.typeClass()),
                    (List<Type>) parametersGetter.invoke(null),
                    JavaSignature.fromMethod(instanceGetter)
                ));
            } catch (ReflectiveOperationException exception) {
                throw new TypeInstanceReflectionError(exception);
            }
        });
    }

    private Optional<Method> findMethod(Class<?> clazz, Class<? extends Annotation> annotation) {
        return stream(clazz.getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(annotation))
            .findFirst();
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

    private ImmutableEntryBuilder getBuilder(String memberName) {
        return builders.computeIfAbsent(qualify(memberName), SymbolEntry::immutableEntry);
    }

    private ImmutableEntryBuilder getBuilder(Symbol memberSymbol) {
        return builders.computeIfAbsent(memberSymbol, SymbolEntry::immutableEntry);
    }

    private void processValues(Class<?> clazz) {
        stream(clazz.getDeclaredMethods()).forEach(method -> {
            Optional.ofNullable(method.getAnnotation(Value.class)).ifPresent(value -> {
                ImmutableEntryBuilder builder = getBuilder(value.memberName());
                builder.withValueSignature(new JavaSignature(p(clazz), method.getName(), sig(method.getReturnType(), method.getParameterTypes())));
                if (value.fixity() != NONE && value.precedence() != -1) {
                    builder.withOperator(operator(value.fixity(), value.precedence()));
                }
            });
            Optional.ofNullable(method.getAnnotation(ValueType.class)).ifPresent(valueType -> {
                ImmutableEntryBuilder builder = getBuilder(valueType.forMember());
                try {
                    builder.withValue((Type) method.invoke(null));
                } catch (ReflectiveOperationException exception) {
                    exception.printStackTrace(); // TODO
                }
            });
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

    private Symbol qualify(String memberName) {
        return qualified(moduleName, memberName);
    }
}
