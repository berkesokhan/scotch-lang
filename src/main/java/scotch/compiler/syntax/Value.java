package scotch.compiler.syntax;

import static java.util.Collections.emptyList;
import static scotch.compiler.text.SourceRange.NULL_SOURCE;
import static scotch.util.StringUtil.stringify;

import java.util.List;
import java.util.Objects;
import com.google.common.collect.ImmutableList;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.text.SourceRange;

public abstract class Value {

    public static Apply apply(Value function, Value argument, Type type) {
        return new Apply(function.getSourceRange().extend(argument.getSourceRange()), function, argument, type);
    }

    public static Identifier id(SourceRange sourceRange, Symbol symbol, Type type) {
        return new Identifier(sourceRange, symbol, type);
    }

    public static LiteralValue literal(SourceRange sourceRange, Object value, Type type) {
        return new LiteralValue(sourceRange, value, type);
    }

    public static Message message(SourceRange sourceRange, List<Value> members) {
        return new Message(sourceRange, members);
    }

    public static PatternMatchers emptyPatterns(Type type) {
        return patterns(NULL_SOURCE, type, emptyList());
    }

    public static PatternMatchers patterns(SourceRange sourceRange, Type type, List<PatternMatcher> patterns) {
        return new PatternMatchers(sourceRange, patterns, type);
    }

    private Value() {
        // intentionally empty
    }

    public abstract <T> T accept(ValueVisitor<T> visitor);

    @Override
    public abstract boolean equals(Object o);

    public abstract SourceRange getSourceRange();

    public abstract Type getType();

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();

    public abstract Value withType(Type type);

    public interface ValueVisitor<T> {

        default T visit(Apply apply) {
            return visitOtherwise(apply);
        }

        default T visit(Identifier identifier) {
            return visitOtherwise(identifier);
        }

        default T visit(LiteralValue literal) {
            return visitOtherwise(literal);
        }

        default T visit(Message message) {
            return visitOtherwise(message);
        }

        default T visit(PatternMatchers matchers) {
            return visitOtherwise(matchers);
        }

        default T visitOtherwise(Value value) {
            throw new UnsupportedOperationException("Can't visit " + value.getClass().getSimpleName());
        }
    }

    public static class Apply extends Value {

        private final SourceRange sourceRange;
        private final Value       function;
        private final Value       argument;
        private final Type        type;

        private Apply(SourceRange sourceRange, Value function, Value argument, Type type) {
            this.sourceRange = sourceRange;
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
        public SourceRange getSourceRange() {
            return sourceRange;
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
            return new Apply(sourceRange, function, argument, type);
        }

        public Apply withFunction(Value function) {
            return new Apply(sourceRange, function, argument, type);
        }

        public Apply withSourceRange(SourceRange sourceRange) {
            return new Apply(sourceRange, function, argument, type);
        }

        @Override
        public Apply withType(Type type) {
            return new Apply(sourceRange, function, argument, type);
        }
    }

    public static class Identifier extends Value {

        private final SourceRange sourceRange;
        private final Symbol      symbol;
        private final Type        type;

        private Identifier(SourceRange sourceRange, Symbol symbol, Type type) {
            this.sourceRange = sourceRange;
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

        @Override
        public SourceRange getSourceRange() {
            return sourceRange;
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

        public Identifier withSourceRange(SourceRange sourceRange) {
            return new Identifier(sourceRange, symbol, type);
        }

        public Identifier withSymbol(Symbol symbol) {
            return new Identifier(sourceRange, symbol, type);
        }

        public Identifier withType(Type type) {
            return new Identifier(sourceRange, symbol, type);
        }
    }

    public static class LiteralValue extends Value {

        private final SourceRange sourceRange;
        private final Object      value;
        private final Type        type;

        private LiteralValue(SourceRange sourceRange, Object value, Type type) {
            this.sourceRange = sourceRange;
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
        public SourceRange getSourceRange() {
            return sourceRange;
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

        public LiteralValue withSourceRange(SourceRange sourceRange) {
            return new LiteralValue(sourceRange, value, type);
        }

        public LiteralValue withType(Type type) {
            return new LiteralValue(sourceRange, value, type);
        }
    }

    public static class Message extends Value {

        private final SourceRange sourceRange;
        private final List<Value> members;

        private Message(SourceRange sourceRange, List<Value> members) {
            this.sourceRange = sourceRange;
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

        public List<Value> getMembers() {
            return members;
        }

        @Override
        public SourceRange getSourceRange() {
            return sourceRange;
        }

        @Override
        public Type getType() {
            throw new IllegalStateException();
        }

        @Override
        public int hashCode() {
            return Objects.hash(members);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + members + ")";
        }

        @Override
        public Value withType(Type type) {
            throw new UnsupportedOperationException();
        }

        public Message withSourceRange(SourceRange sourceRange) {
            return new Message(sourceRange, members);
        }
    }

    public static class PatternMatchers extends Value {

        private final SourceRange          sourceRange;
        private final List<PatternMatcher> matchers;
        private final Type                 type;

        private PatternMatchers(SourceRange sourceRange, List<PatternMatcher> matchers, Type type) {
            this.sourceRange = sourceRange;
            this.matchers = matchers;
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
            } else if (o instanceof PatternMatchers) {
                PatternMatchers other = (PatternMatchers) o;
                return Objects.equals(matchers, other.matchers)
                    && Objects.equals(type, other.type);
            } else {
                return false;
            }
        }

        public List<PatternMatcher> getMatchers() {
            return matchers;
        }

        @Override
        public SourceRange getSourceRange() {
            return sourceRange;
        }

        @Override
        public Type getType() {
            return type;
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
            return new PatternMatchers(sourceRange, matchers, type);
        }

        public PatternMatchers withSourceRange(SourceRange sourceRange) {
            return new PatternMatchers(sourceRange, matchers, type);
        }

        @Override
        public PatternMatchers withType(Type type) {
            return new PatternMatchers(sourceRange, matchers, type);
        }
    }
}
