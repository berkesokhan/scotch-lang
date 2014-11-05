package scotch.compiler.syntax;

import static java.util.Arrays.asList;
import static scotch.compiler.syntax.Symbol.fromString;
import static scotch.compiler.util.TextUtil.stringify;

import java.util.List;
import java.util.Objects;
import com.google.common.collect.ImmutableList;

public abstract class Value {

    public static Value apply(Value function, Value argument, Type type) {
        return new Apply(function, argument, type);
    }

    public static Value id(String name, Type type) {
        return id(fromString(name), type);
    }

    public static Value id(Symbol symbol, Type type) {
        return new Identifier(symbol, type);
    }

    public static Value literal(Object value, Type type) {
        return new LiteralValue(value, type);
    }

    public static Value message(Value... members) {
        return message(asList(members));
    }

    public static Value message(List<Value> members) {
        return new Message(members);
    }

    public static Value patterns(Type type, PatternMatcher... patterns) {
        return patterns(type, asList(patterns));
    }

    public static Value patterns(Type type, List<PatternMatcher> patterns) {
        return new PatternMatchers(patterns, type);
    }

    private Value() {
        // intentionally empty
    }

    public abstract <T> T accept(ValueVisitor<T> visitor);

    @Override
    public abstract boolean equals(Object o);

    public abstract Type getType();

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();

    public interface ValueVisitor<T> {

        default T visit(Apply apply) {
            return visitOtherwise(apply);
        }

        default T visit(Identifier identifier) {
            return visitOtherwise(identifier);
        }

        default T visit(LiteralValue value) {
            return visitOtherwise(value);
        }

        default T visit(Message message) {
            return visitOtherwise(message);
        }

        default T visit(PatternMatchers matchers) {
            return visitOtherwise(matchers);
        }

        default T visitOtherwise(Value value) {
            throw new UnsupportedOperationException("Can't visit " + value);
        }
    }

    public static class Apply extends Value {

        private final Value function;
        private final Value argument;
        private final Type  type;

        private Apply(Value function, Value argument, Type type) {
            this.function = function;
            this.argument = argument;
            this.type = type;
        }

        @Override
        public <T> T accept(ValueVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof Apply) {
                Apply other = (Apply) o;
                return Objects.equals(function, other.function)
                    && Objects.equals(argument, other.argument)
                    && Objects.equals(type, other.type);
            } else {
                return false;
            }
        }

        public Value getArgument() {
            return argument;
        }

        public Value getFunction() {
            return function;
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(function, argument, type);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + function + ", " + argument + ")";
        }

        public Apply withArgument(Value argument) {
            return new Apply(function, argument, type);
        }

        public Apply withFunction(Value function) {
            return new Apply(function, argument, type);
        }

        public Value withType(Type type) {
            return new Apply(function, argument, type);
        }
    }

    public static class Identifier extends Value {

        private final Symbol symbol;
        private final Type   type;

        private Identifier(Symbol symbol, Type type) {
            this.symbol = symbol;
            this.type = type;
        }

        @Override
        public <T> T accept(ValueVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof Identifier) {
                Identifier other = (Identifier) o;
                return Objects.equals(symbol, other.symbol)
                    && Objects.equals(type, other.type);
            } else {
                return false;
            }
        }

        public Symbol getSymbol() {
            return symbol;
        }

        public Type getType() {
            return type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol, type);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + symbol + ")";
        }

        public Identifier withSymbol(Symbol symbol) {
            return new Identifier(symbol, type);
        }

        public Identifier withType(Type type) {
            return new Identifier(symbol, type);
        }
    }

    public static class LiteralValue extends Value {

        private final Object value;
        private final Type   type;

        private LiteralValue(Object value, Type type) {
            this.value = value;
            this.type = type;
        }

        @Override
        public <T> T accept(ValueVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof LiteralValue) {
                LiteralValue other = (LiteralValue) o;
                return Objects.equals(value, other.value)
                    && Objects.equals(type, other.type);
            } else {
                return false;
            }
        }

        @Override
        public Type getType() {
            return type;
        }

        public Object getValue() {
            return value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value, type);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + value + ")";
        }

        public LiteralValue withType(Type type) {
            return new LiteralValue(value, type);
        }
    }

    public static class Message extends Value {

        private final List<Value> members;

        private Message(List<Value> members) {
            this.members = ImmutableList.copyOf(members);
        }

        @Override
        public <T> T accept(ValueVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof Message && Objects.equals(members, ((Message) o).members);
        }

        @Override
        public Type getType() {
            throw new IllegalStateException();
        }

        public List<Value> getMembers() {
            return members;
        }

        @Override
        public int hashCode() {
            return Objects.hash(members);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + members + ")";
        }
    }

    public static class PatternMatchers extends Value {

        private final List<PatternMatcher> matchers;
        private final Type                 type;

        private PatternMatchers(List<PatternMatcher> matchers, Type type) {
            this.matchers = ImmutableList.copyOf(matchers);
            this.type = type;
        }

        @Override
        public <T> T accept(ValueVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            return o == this || o instanceof PatternMatchers && Objects.equals(matchers, ((PatternMatchers) o).matchers);
        }

        @Override
        public Type getType() {
            return type;
        }

        public List<PatternMatcher> getMatchers() {
            return matchers;
        }

        @Override
        public int hashCode() {
            return Objects.hash(matchers);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + matchers + ")";
        }

        public PatternMatchers withMatchers(List<PatternMatcher> matchers) {
            return new PatternMatchers(matchers, type);
        }

        public PatternMatchers withType(Type type) {
            return new PatternMatchers(matchers, type);
        }
    }
}
