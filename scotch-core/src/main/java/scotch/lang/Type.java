package scotch.lang;

import static java.lang.Character.isLowerCase;
import static java.lang.String.join;
import static java.util.Collections.emptyList;
import static scotch.compiler.util.TextUtil.stringify;

import java.util.List;
import java.util.Objects;
import com.google.common.collect.ImmutableList;
import scotch.data.list.PersistentList;

public abstract class Type {

    public static Type fn(Type argument, Type result) {
        return new FunctionType(argument, result);
    }

    public static Type sum(String symbol) {
        return new SumType(symbol);
    }

    public static Type sum(String symbol, PersistentList<Type> arguments) {
        return new SumType(symbol);
    }

    public static Type t(int id) {
        return var("t" + id);
    }

    public static VariableType var(String symbol) {
        return var(symbol, emptyList());
    }

    public static VariableType var(String symbol, List<String> context) {
        return new VariableType(symbol, context);
    }

    private Type() {
        // intentionally empty
    }

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();

    public interface TypeVisitor<T> {

        default T visit(FunctionType type) {
            return visitOtherwise(type);
        }

        default T visit(SumType type) {
            return visitOtherwise(type);
        }

        default T visit(VariableType type) {
            return visitOtherwise(type);
        }

        default T visitOtherwise(Type type) {
            throw new UnsupportedOperationException("Can't visit " + type.getClass().getSimpleName());
        }
    }

    public static class FunctionType extends Type {

        private final Type argument;
        private final Type result;

        public FunctionType(Type argument, Type result) {
            this.argument = argument;
            this.result = result;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof FunctionType) {
                FunctionType other = (FunctionType) o;
                return Objects.equals(argument, other.argument)
                    && Objects.equals(result, other.result);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(argument, result);
        }

        @Override
        public String toString() {
            return "(" + argument + " -> " + result + ")"; // TODO formatting
        }
    }

    public static class SumType extends Type {

        private final String symbol;

        private SumType(String symbol) {
            this.symbol = symbol;
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

        @Override
        public int hashCode() {
            return Objects.hash(symbol);
        }

        @Override
        public String toString() {
            return symbol;
        }
    }

    public static class VariableType extends Type {

        private final String       symbol;
        private final List<String> context;

        private VariableType(String symbol, List<String> context) {
            if (!isLowerCase(symbol.charAt(0))) {
                throw new IllegalArgumentException("Variable type should have lower-case name: got '" + symbol + "'");
            }
            this.symbol = symbol;
            this.context = ImmutableList.copyOf(context);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof VariableType) {
                VariableType other = (VariableType) o;
                return Objects.equals(symbol, other.symbol)
                    && Objects.equals(context, other.context);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol, context);
        }

        @Override
        public String toString() {
            if (context.isEmpty()) {
                return stringify(this) + "(" + symbol + ")";
            } else {
                return stringify(this) + "(" + symbol + " of [" + join(", ", context) + "])";
            }
        }
    }
}
