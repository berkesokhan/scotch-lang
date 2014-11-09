package scotch.compiler.syntax;

import static java.lang.Character.isLowerCase;
import static java.lang.Character.isUpperCase;
import static java.util.Collections.emptyList;
import static scotch.compiler.syntax.SourceRange.NULL_SOURCE;
import static scotch.compiler.syntax.Symbol.fromString;
import static scotch.compiler.syntax.Unification.circular;
import static scotch.compiler.syntax.Unification.mismatch;
import static scotch.compiler.syntax.Unification.unified;
import static scotch.compiler.util.TextUtil.stringify;

import java.util.List;
import java.util.Objects;

public abstract class Type implements SourceAware<Type> {

    public static Type fn(Type argument, Type result) {
        return new FunctionType(NULL_SOURCE, argument, result);
    }

    public static Type sum(String name) {
        return sum(name, emptyList());
    }

    public static Type sum(String name, List<Type> arguments) {
        return sum(fromString(name));
    }

    public static Type sum(Symbol symbol) {
        return new SumType(NULL_SOURCE, symbol);
    }

    public static Type t(int id) {
        return var("t" + id);
    }

    public static VariableType var(String name) {
        return new VariableType(NULL_SOURCE, name);
    }

    private static Unification unifyVariable(Type target, VariableType variableType, TypeScope typeScope) {
        if (typeScope.isBound(variableType)) {
            return typeScope.getTarget(variableType).unify(target, typeScope);
        } else {
            typeScope.bind(variableType, target);
            return unified(target);
        }
    }

    private Type() {
        // intentionally Optional.empty
    }

    public abstract <T> T accept(TypeVisitor<T> visitor);

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    public abstract String prettyPrint();

    @Override
    public abstract String toString();

    public abstract Unification unify(Type type, TypeScope scope);

    protected abstract boolean contains(VariableType variableType);

    protected abstract Unification unifyWith(SumType target, TypeScope typeScope);

    protected abstract Unification unifyWith(VariableType target, TypeScope typeScope);

    protected abstract Unification unifyWith(FunctionType target, TypeScope typeScope);

    public interface TypeVisitor<T> {

        default T visit(FunctionType type) {
            return visitOtherwise(type);
        }

        default T visit(VariableType type) {
            return visitOtherwise(type);
        }

        default T visit(SumType type) {
            return visitOtherwise(type);
        }

        default T visitOtherwise(Type type) {
            throw new UnsupportedOperationException("Can't visit " + type.getClass().getSimpleName());
        }
    }

    public static class FunctionType extends Type {

        private final SourceRange sourceRange;
        private final Type        argument;
        private final Type        result;

        private FunctionType(SourceRange sourceRange, Type argument, Type result) {
            this.sourceRange = sourceRange;
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
        public String prettyPrint() {
            return argument.accept(new TypeVisitor<String>() {
                @Override
                public String visit(FunctionType type) {
                    return "(" + type.prettyPrint() + ")";
                }

                @Override
                public String visitOtherwise(Type type) {
                    return type.prettyPrint();
                }
            }) + " -> " + result.prettyPrint();
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + argument + " -> " + result + ")";
        }

        @Override
        public Unification unify(Type type, TypeScope scope) {
            return type.unifyWith(this, scope);
        }

        public FunctionType withArgument(Type argument) {
            return new FunctionType(sourceRange, argument, result);
        }

        public FunctionType withResult(Type result) {
            return new FunctionType(sourceRange, argument, result);
        }

        @Override
        public FunctionType withSourceRange(SourceRange sourceRange) {
            return new FunctionType(sourceRange, argument, result);
        }

        @Override
        protected boolean contains(VariableType variableType) {
            return argument.contains(variableType) || result.contains(variableType);
        }

        @Override
        protected Unification unifyWith(SumType target, TypeScope typeScope) {
            return mismatch(target, this);
        }

        @Override
        protected Unification unifyWith(VariableType target, TypeScope typeScope) {
            return unifyVariable(this, target, typeScope);
        }

        @Override
        protected Unification unifyWith(FunctionType target, TypeScope typeScope) {
            return target.argument.unify(argument, typeScope).andThen(
                argumentResult -> target.result.unify(result, typeScope).andThen(
                    resultResult -> unified(fn(argumentResult, resultResult))
                )
            );
        }
    }

    public static class SumType extends Type {

        private final SourceRange sourceRange;
        private final Symbol      symbol;

        private SumType(SourceRange sourceRange, Symbol symbol) {
            shouldBeUpperCase(symbol.getMemberName());
            this.sourceRange = sourceRange;
            this.symbol = symbol;
        }

        @Override
        public <T> T accept(TypeVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof SumType) {
                SumType other = (SumType) o;
                return Objects.equals(symbol, other.symbol);
            } else {
                return false;
            }
        }

        public SourceRange getSourceRange() {
            return sourceRange;
        }

        public Symbol getSymbol() {
            return symbol;
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol);
        }

        @Override
        public String prettyPrint() {
            return symbol.getCanonicalName();
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + symbol + ")";
        }

        @Override
        public Unification unify(Type type, TypeScope scope) {
            return type.unifyWith(this, scope);
        }

        @Override
        public SumType withSourceRange(SourceRange sourceRange) {
            return new SumType(sourceRange, symbol);
        }

        public SumType withSymbol(Symbol symbol) {
            return new SumType(sourceRange, symbol);
        }

        private void shouldBeUpperCase(String name) {
            if (!isUpperCase(name.charAt(0))) {
                throw new IllegalArgumentException("Sum type should have upper-case name: got '" + name + "'");
            }
        }

        @Override
        protected boolean contains(VariableType variableType) {
            return false;
        }

        @Override
        protected Unification unifyWith(SumType target, TypeScope typeScope) {
            if (equals(target)) {
                return unified(this);
            } else {
                return mismatch(target, this);
            }
        }

        @Override
        protected Unification unifyWith(VariableType target, TypeScope typeScope) {
            return unifyVariable(this, target, typeScope);
        }

        @Override
        protected Unification unifyWith(FunctionType target, TypeScope typeScope) {
            return mismatch(target, this);
        }
    }

    public static class VariableType extends Type {

        private final SourceRange sourceRange;
        private final String      name;

        private VariableType(SourceRange sourceRange, String name) {
            if (!isLowerCase(name.charAt(0))) {
                throw new IllegalArgumentException("Variable type should have lower-case name: got '" + name + "'");
            }
            this.sourceRange = sourceRange;
            this.name = name;
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
                return Objects.equals(name, other.name);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public String prettyPrint() {
            return name;
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + name + ")";
        }

        @Override
        public Unification unify(Type type, TypeScope scope) {
            return type.unifyWith(this, scope);
        }

        @Override
        public VariableType withSourceRange(SourceRange sourceRange) {
            return new VariableType(sourceRange, name);
        }

        private Unification bind(Type target, TypeScope typeScope) {
            typeScope.bind(this, target);
            return unified(target);
        }

        private Unification unify_(Type target, TypeScope typeScope) {
            if (typeScope.isBound(this)) {
                return target.unify(typeScope.getTarget(this), typeScope);
            } else if (target.contains(this)) {
                return circular(target, this);
            } else {
                return bind(target, typeScope);
            }
        }

        @Override
        protected boolean contains(VariableType variableType) {
            return equals(variableType);
        }

        @Override
        protected Unification unifyWith(SumType target, TypeScope typeScope) {
            return unify_(target, typeScope);
        }

        @Override
        protected Unification unifyWith(VariableType target, TypeScope typeScope) {
            return unify_(target, typeScope);
        }

        @Override
        protected Unification unifyWith(FunctionType target, TypeScope typeScope) {
            return unify_(target, typeScope);
        }
    }
}
