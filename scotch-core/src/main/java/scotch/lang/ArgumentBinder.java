package scotch.lang;

import static java.util.stream.Collectors.*;
import static scotch.lang.Either.*;
import static scotch.lang.Type.*;
import static scotch.lang.Unification.*;

import java.util.*;
import java.util.function.Function;
import scotch.lang.Type.*;

public class ArgumentBinder {

    private final Map<Type, Type> argumentBindings = new LinkedHashMap<>();
    private final UnionType unionType;
    private final TypeScope typeScope;

    public ArgumentBinder(TypeScope typeScope, Type genericType, List<Type> arguments) {
        this.typeScope = typeScope;
        this.unionType = genericType.accept(new TypeVisitor<UnionType>() {
            @Override
            public UnionType visit(UnionType unionType) {
                int i = 0;
                for (Type argument : unionType.getArguments()) {
                    argumentBindings.put(argument, arguments.get(i++));
                }
                return unionType;
            }
        });
    }

    public Unification bind() {
        List<Function<UnionType, UnionMember>> members = new ArrayList<>();
        for (UnionMember member : unionType.getMembers()) {
            Either<Unification, Function<UnionType, UnionMember>> result = bindMember(member);
            if (result.isRight()) {
                members.add(result.getRight());
            } else {
                return result.getLeft();
            }
        }
        return unified(typeScope.generate(union(unionType.getName(), new ArrayList<>(argumentBindings.values()), members)));
    }

    private Either<Unification, Function<UnionType, UnionMember>> bindMember(UnionMember member) {
        List<MemberField> fields = new ArrayList<>();
        for (MemberField field : member.getFields()) {
            Unification status = bind_(field.getType()).andThen(type -> {
                fields.add(field(field.getName(), type));
                return unified(type);
            });
            if (!status.isUnified()) {
                return left(status);
            }
        }
        return right(ctor(
            member.getName(),
            member.getArguments().stream().map(argumentBindings::get).collect(toList()),
            fields
        ));
    }

    private Unification bind_(Type type) {
        return type.accept(new TypeVisitor<Unification>() {
            @Override
            public Unification visit(VariableType variableType) {
                if (argumentBindings.containsKey(variableType)) {
                    Unification result = variableType.unify(argumentBindings.get(variableType), typeScope);
                    new HashSet<>(argumentBindings.keySet()).forEach(key -> argumentBindings.put(key, typeScope.getTarget(key)));
                    return result;
                } else {
                    return unified(variableType);
                }
            }

            @Override
            public Unification visit(RecursiveLookup lookup) {
                return unified(lookup(
                    lookup.getName(),
                    lookup.getArguments().stream()
                        .map(argument -> argumentBindings.getOrDefault(argument, argument))
                        .collect(toList())
                ));
            }
        });
    }
}
