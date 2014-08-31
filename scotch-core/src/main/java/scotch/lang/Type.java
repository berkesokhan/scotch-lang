package scotch.lang;

import static java.lang.Character.*;
import static java.lang.String.*;
import static java.util.Collections.*;
import static java.util.Optional.*;
import static java.util.stream.Collectors.*;
import static scotch.lang.Unification.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import com.google.common.collect.ImmutableList;

public abstract class Type {

    public static Type argumentOf(Type maybeFunction) {
        return maybeFunction.accept(new TypeVisitor<Type>() {
            @Override
            public Type visit(FunctionType functionType) {
                return functionType.argument;
            }

            @Override
            public Type visitOtherwise(Type type) {
                throw new IllegalArgumentException("Can't get argument of " + type);
            }
        });
    }

    public static Function<UnionType, UnionMember> constant(String name) {
        return ctor(name, emptyList());
    }

    public static Function<UnionType, UnionMember> ctor(String name, List<MemberField> fields) {
        return ctor(name, emptyList(), fields);
    }

    public static Function<UnionType, UnionMember> ctor(String name, List<Type> arguments, List<MemberField> fields) {
        return parent -> new UnionMember(name, parent, arguments, fields);
    }

    public static MemberField field(String name, Type type) {
        return new MemberField(name, type);
    }

    public static Type fn(Type argument, Type result) {
        return new FunctionType(argument, result);
    }

    public static Type lookup(String name, List<Type> arguments) {
        return new UnionLookup(name, arguments);
    }

    public static Type nullary(String name) {
        return union(name, emptyList(), emptyList());
    }

    public static Type union(String name, List<Function<UnionType, UnionMember>> members) {
        return union(name, emptyList(), members);
    }

    public static Type union(String name, List<Type> arguments, List<Function<UnionType, UnionMember>> members) {
        return new UnionType(name, arguments, members);
    }

    public static VariableType var(String name) {
        return var(name, emptyList());
    }

    public static VariableType var(String name, List<String> context) {
        return new VariableType(name, context);
    }

    private static Unification unifyVariable(Type target, VariableType variableType, TypeScope typeScope) {
        if (typeScope.isBound(variableType)) {
            return typeScope.getTarget(variableType).unify(target, typeScope);
        } else if (typeScope.contextsMatch(target, variableType)) {
            typeScope.bind(variableType, target);
            return unified(target);
        } else {
            return mismatch(variableType, target, typeScope);
        }
    }

    private Type() {
        // intentionally empty
    }

    public abstract <T> T accept(TypeVisitor<T> visitor);

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();

    public abstract Unification unify(Type type, TypeScope typeScope);

    protected abstract boolean contains(VariableType variableType);

    protected abstract Unification unifyWith(UnionType unionType, TypeScope typeScope);

    protected abstract Unification unifyWith(VariableType variableType, TypeScope typeScope);

    protected abstract Unification unifyWith(FunctionType functionType, TypeScope typeScope);

    protected abstract Unification unifyWith(UnionLookup lookup, TypeScope typeScope);

    public interface TypeVisitor<T> {

        default T visit(FunctionType functionType) {
            return visitOtherwise(functionType);
        }

        default T visit(VariableType variableType) {
            return visitOtherwise(variableType);
        }

        default T visit(UnionType unionType) {
            return visitOtherwise(unionType);
        }

        default T visit(UnionLookup lookup) {
            return visitOtherwise(lookup);
        }

        default T visitOtherwise(Type type) {
            throw new UnsupportedOperationException("Can't visit " + type.getClass().getSimpleName());
        }
    }

    public static class FunctionType extends Type {

        private final Type argument;
        private final Type result;

        private FunctionType(Type argument, Type result) {
            this.argument = argument;
            this.result = result;
        }

        @Override
        public <T> T accept(TypeVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof FunctionType) {
                FunctionType other = (FunctionType) o;
                return argument.equals(other.argument) && result.equals(other.result);
            } else {
                return false;
            }
        }

        public Type getArgument() {
            return argument;
        }

        public Type getResult() {
            return result;
        }

        @Override
        public int hashCode() {
            return Objects.hash(argument, result);
        }

        @Override
        public String toString() {
            return "Function(" + argument + " -> " + result + ")";
        }

        @Override
        public Unification unify(Type type, TypeScope typeScope) {
            return type.unifyWith(this, typeScope);
        }

        @Override
        protected boolean contains(VariableType variableType) {
            return argument.contains(variableType) || result.contains(variableType);
        }

        @Override
        protected Unification unifyWith(UnionType unionType, TypeScope typeScope) {
            return mismatch(unionType, this);
        }

        @Override
        protected Unification unifyWith(VariableType variableType, TypeScope typeScope) {
            return unifyVariable(this, variableType, typeScope);
        }

        @Override
        protected Unification unifyWith(FunctionType functionType, TypeScope typeScope) {
            return functionType.argument.unify(argument, typeScope).andThen(
                argumentResult -> functionType.result.unify(functionType.result, typeScope).andThen(
                    resultResult -> unified(fn(argumentResult, resultResult))
                )
            );
        }

        @Override
        protected Unification unifyWith(UnionLookup lookup, TypeScope typeScope) {
            return mismatch(lookup, this);
        }
    }

    public static class MemberField {

        private final String name;
        private final Type type;

        private MemberField(String name, Type type) {
            this.name = name;
            this.type = type;
        }

        public boolean contains(VariableType variableType) {
            return type.contains(variableType);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof MemberField) {
                MemberField other = (MemberField) o;
                return Objects.equals(name, other.name)
                    && Objects.equals(type, other.type);
            } else {
                return false;
            }
        }

        public String getName() {
            return name;
        }

        public Type getType() {
            return type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type);
        }
    }

    public static class UnionLookup extends Type {

        private final String name;
        private final List<Type> arguments;

        private UnionLookup(String name, List<Type> arguments) {
            this.name = name;
            this.arguments = arguments;
        }

        @Override
        public <T> T accept(TypeVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof UnionLookup) {
                UnionLookup other = (UnionLookup) o;
                return Objects.equals(name, other.name)
                    && Objects.equals(arguments, other.arguments);
            } else {
                return false;
            }
        }

        public List<Type> getArguments() {
            return arguments;
        }

        public String getName() {
            return name;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, arguments);
        }

        @Override
        public String toString() {
            return "UnionLookup(" + name + ")";
        }

        @Override
        public Unification unify(Type type, TypeScope typeScope) {
            return type.unifyWith(this, typeScope);
        }

        @Override
        protected boolean contains(VariableType variableType) {
            return arguments.contains(variableType);
        }

        @Override
        protected Unification unifyWith(UnionType unionType, TypeScope typeScope) {
            if (Objects.equals(name, unionType.name) && Objects.equals(arguments, unionType.arguments)) {
                return unified(unionType);
            } else {
                return mismatch(unionType, this);
            }
        }

        @Override
        protected Unification unifyWith(VariableType variableType, TypeScope typeScope) {
            return unifyVariable(this, variableType, typeScope);
        }

        @Override
        protected Unification unifyWith(FunctionType functionType, TypeScope typeScope) {
            return mismatch(functionType, this);
        }

        @Override
        protected Unification unifyWith(UnionLookup lookup, TypeScope typeScope) {
            if (equals(lookup)) {
                return unified(this);
            } else {
                return mismatch(lookup, this);
            }
        }
    }

    public static class UnionMember {

        private final String name;
        private final UnionType parent;
        private final List<Type> arguments;
        private final List<MemberField> fields;

        private UnionMember(String name, UnionType parent, List<Type> arguments, List<MemberField> fields) {
            if (parent.arguments.size() < arguments.size()) {
                throw new IllegalArgumentException("UnionMember received more arguments than parent type " + parent
                    + "; got: " + arguments + " but expected " + parent.arguments);
            }
            this.name = name;
            this.parent = parent;
            this.arguments = arguments;
            this.fields = fields;
        }

        public boolean contains(VariableType variableType) {
            return fields.stream().anyMatch(field -> field.contains(variableType));
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof UnionMember) {
                UnionMember other = (UnionMember) o;
                return parent.shallowEquals(other.parent)
                    && Objects.equals(name, other.name)
                    && Objects.equals(fields, other.fields);
            } else {
                return false;
            }
        }

        public List<Type> getArguments() {
            return arguments;
        }

        public List<MemberField> getFields() {
            return fields;
        }

        public String getName() {
            return name;
        }

        @Override
        public int hashCode() {
            return Objects.hash(parent.shallowHashCode(), name, fields);
        }

        @Override
        public String toString() {
            return "UnionMember(" + name + ")";
        }
    }

    public static class UnionType extends Type {

        private final String name;
        private final List<Type> arguments;
        private final List<UnionMember> members;

        private UnionType(String name, List<Type> arguments, List<Function<UnionType, UnionMember>> members) {
            if (!isUpperCase(name.charAt(0))) {
                throw new IllegalArgumentException("Union type should have upper-case name: got '" + name + "'");
            }
            this.name = name;
            this.arguments = ImmutableList.copyOf(arguments);
            this.members = ImmutableList.copyOf(members.stream().map(fn -> fn.apply(this)).collect(toList()));
        }

        @Override
        public <T> T accept(TypeVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof UnionType) {
                UnionType other = (UnionType) o;
                return Objects.equals(name, other.name)
                    && Objects.equals(members, other.members);
            } else {
                return false;
            }
        }

        public List<Type> getArguments() {
            return arguments;
        }

        public List<UnionMember> getMembers() {
            return members;
        }

        public String getName() {
            return name;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, arguments, members);
        }

        public int shallowHashCode() {
            return Objects.hash(name, arguments);
        }

        @Override
        public String toString() {
            return "Union(" + name + ")";
        }

        @Override
        public Unification unify(Type type, TypeScope typeScope) {
            return type.unifyWith(this, typeScope);
        }

        private boolean shallowEquals(UnionType type) {
            return Objects.equals(name, type.name);
        }

        @Override
        protected boolean contains(VariableType variableType) {
            return members.stream().anyMatch(member -> member.contains(variableType));
        }

        @Override
        protected Unification unifyWith(UnionType unionType, TypeScope typeScope) {
            if (equals(unionType)) {
                return unified(this);
            } else {
                return mismatch(unionType, this);
            }
        }

        @Override
        protected Unification unifyWith(VariableType variableType, TypeScope typeScope) {
            return unifyVariable(this, variableType, typeScope);
        }

        @Override
        protected Unification unifyWith(FunctionType functionType, TypeScope typeScope) {
            return mismatch(functionType, this);
        }

        @Override
        protected Unification unifyWith(UnionLookup lookup, TypeScope typeScope) {
            if (Objects.equals(name, lookup.name) && Objects.equals(arguments, lookup.arguments)) {
                return unified(this);
            } else {
                return mismatch(lookup, this);
            }
        }
    }

    public static class VariableType extends Type {

        private final String name;
        private final List<String> context;

        private VariableType(String name, List<String> context) {
            if (!isLowerCase(name.charAt(0))) {
                throw new IllegalArgumentException("Variable type should have lower-case name: got '" + name + "'");
            }
            this.name = name;
            this.context = ImmutableList.copyOf(context);
        }

        @Override
        public <T> T accept(TypeVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof VariableType) {
                VariableType other = (VariableType) o;
                return Objects.equals(name, other.name)
                    && Objects.equals(context, other.context);
            } else {
                return false;
            }
        }

        public List<String> getContext() {
            return context;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, context);
        }

        @Override
        public String toString() {
            if (context.isEmpty()) {
                return "Variable(" + name + ")";
            } else {
                return "Variable(" + name + " of [" + join(", ", context) + "])";
            }
        }

        @Override
        public Unification unify(Type type, TypeScope typeScope) {
            return type.unifyWith(this, typeScope);
        }

        private Unification bind(Type type, TypeScope typeScope) {
            if (typeScope.contextsMatch(type, this)) {
                typeScope.bind(this, type);
                return unified(type);
            } else {
                return mismatch(type, this, typeScope);
            }
        }

        private Optional<Unification> unify_(Type type, TypeScope typeScope) {
            if (typeScope.isBound(this)) {
                return of(type.unify(typeScope.getTarget(this), typeScope));
            } else if (type.contains(this)) {
                return of(circular(type, this));
            } else {
                return empty();
            }
        }

        @Override
        protected boolean contains(VariableType variableType) {
            return equals(variableType);
        }

        @Override
        protected Unification unifyWith(UnionType unionType, TypeScope typeScope) {
            return unify_(unionType, typeScope).orElseGet(() -> bind(unionType, typeScope));
        }

        @Override
        protected Unification unifyWith(VariableType variableType, TypeScope typeScope) {
            return unify_(variableType, typeScope).orElseGet(() -> {
                if (context.isEmpty() && variableType.context.isEmpty()) {
                    return bind(variableType, typeScope);
                } else {
                    List<String> unifiedContext = new ArrayList<>();
                    unifiedContext.addAll(variableType.context);
                    unifiedContext.addAll(context);
                    Type target = typeScope.reserve(unifiedContext);
                    typeScope.bind(variableType, target);
                    typeScope.bind(this, target);
                    return unified(target);
                }
            });
        }

        @Override
        protected Unification unifyWith(FunctionType functionType, TypeScope typeScope) {
            return unify_(functionType, typeScope).orElseGet(() -> bind(functionType, typeScope));
        }

        @Override
        protected Unification unifyWith(UnionLookup lookup, TypeScope typeScope) {
            return unify_(lookup, typeScope).orElseGet(() -> bind(lookup, typeScope));
        }
    }
}
