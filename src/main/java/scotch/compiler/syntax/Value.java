package scotch.compiler.syntax;

import static java.util.Collections.reverse;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static scotch.compiler.symbol.Symbol.unqualified;
import static scotch.compiler.symbol.Type.sum;
import static scotch.compiler.syntax.DefinitionReference.scopeRef;
import static scotch.compiler.syntax.DefinitionReference.valueRef;
import static scotch.util.StringUtil.quote;
import static scotch.util.StringUtil.stringify;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import com.google.common.collect.ImmutableList;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.syntax.DefinitionReference.InstanceReference;
import scotch.compiler.syntax.DefinitionReference.ScopeReference;
import scotch.compiler.syntax.DefinitionReference.ValueReference;
import scotch.compiler.text.SourceRange;

public abstract class Value {

    public static Apply apply(Value function, Value argument, Type type) {
        return new Apply(function.getSourceRange().extend(argument.getSourceRange()), function, argument, type);
    }

    public static Argument arg(SourceRange sourceRange, String name, Type type) {
        return new Argument(sourceRange, name, type);
    }

    public static FunctionValue fn(SourceRange sourceRange, Symbol symbol, List<Argument> arguments, Value body) {
        return new FunctionValue(sourceRange, symbol, arguments, body);
    }

    public static Identifier id(SourceRange sourceRange, Symbol symbol, Type type) {
        return new Identifier(sourceRange, symbol, type);
    }

    public static BoolLiteral literal(SourceRange sourceRange, boolean value) {
        return new BoolLiteral(sourceRange, value);
    }

    public static CharLiteral literal(SourceRange sourceRange, char value) {
        return new CharLiteral(sourceRange, value);
    }

    public static DoubleLiteral literal(SourceRange sourceRange, double value) {
        return new DoubleLiteral(sourceRange, value);
    }

    public static IntLiteral literal(SourceRange sourceRange, int value) {
        return new IntLiteral(sourceRange, value);
    }

    public static StringLiteral literal(SourceRange sourceRange, String value) {
        return new StringLiteral(sourceRange, value);
    }

    public static Message message(SourceRange sourceRange, List<Value> members) {
        return new Message(sourceRange, members);
    }

    public static Method method(SourceRange sourceRange, ValueReference valueRef, List<Type> instances, Type type) {
        return new Method(sourceRange, valueRef, instances, type);
    }

    public static PatternMatchers patterns(SourceRange sourceRange, Type type, List<PatternMatcher> patterns) {
        return new PatternMatchers(sourceRange, patterns, type);
    }

    public static UnboundMethod unboundMethod(SourceRange sourceRange, ValueReference valueRef, Type type) {
        return new UnboundMethod(sourceRange, valueRef, type);
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

    public static Instance instance(SourceRange sourceRange, InstanceReference reference, Type type) {
        return new Instance(sourceRange, reference, type);
    }

    @Override
    public abstract String toString();

    public Value unwrap() {
        return this;
    }

    public abstract Value withType(Type type);

    public interface ValueVisitor<T> {

        default T visit(Apply apply) {
            return visitOtherwise(apply);
        }

        default T visit(Argument argument) {
            return visitOtherwise(argument);
        }

        default T visit(BoolLiteral literal) {
            return visitOtherwise(literal);
        }

        default T visit(BoundValue boundValue) {
            return visitOtherwise(boundValue);
        }

        default T visit(CharLiteral literal) {
            return visitOtherwise(literal);
        }

        default T visit(DoubleLiteral literal) {
            return visitOtherwise(literal);
        }

        default T visit(FunctionValue function) {
            return visitOtherwise(function);
        }

        default T visit(Identifier identifier) {
            return visitOtherwise(identifier);
        }

        default T visit(IntLiteral literal) {
            return visitOtherwise(literal);
        }

        default T visit(Instance instance) {
            return visitOtherwise(instance);
        }

        default T visit(LambdaValue lambda) {
            return visitOtherwise(lambda);
        }

        default T visit(Message message) {
            return visitOtherwise(message);
        }

        default T visit(Method method) {
            return visitOtherwise(method);
        }

        default T visit(PatternMatchers matchers) {
            return visitOtherwise(matchers);
        }

        default T visit(StringLiteral literal) {
            return visitOtherwise(literal);
        }

        default T visit(UnboundMethod unboundMethod) {
            return visitOtherwise(unboundMethod);
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

    public static class Argument extends Value {

        private final SourceRange sourceRange;
        private final String      name;
        private final Type        type;

        public Argument(SourceRange sourceRange, String name, Type type) {
            this.sourceRange = sourceRange;
            this.name = name;
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
            } else if (o instanceof Argument) {
                Argument other = (Argument) o;
                return Objects.equals(sourceRange, other.sourceRange)
                    && Objects.equals(name, other.name)
                    && Objects.equals(type, other.type);
            } else {
                return false;
            }
        }

        public String getName() {
            return name;
        }

        @Override
        public SourceRange getSourceRange() {
            return sourceRange;
        }

        public Symbol getSymbol() {
            return unqualified(name);
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + name + " :: " + type + ")";
        }

        @Override
        public Argument withType(Type type) {
            return arg(sourceRange, name, type);
        }
    }

    public static class BoolLiteral extends Value {

        private final SourceRange sourceRange;
        private final boolean     value;

        public BoolLiteral(SourceRange sourceRange, boolean value) {
            this.sourceRange = sourceRange;
            this.value = value;
        }

        @Override
        public <T> T accept(ValueVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof BoolLiteral) {
                BoolLiteral other = (BoolLiteral) o;
                return Objects.equals(sourceRange, other.sourceRange)
                    && value == other.value;
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
            return sum("scotch.data.bool.Bool");
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + quote(value) + ")";
        }

        @Override
        public Value withType(Type type) {
            return this;
        }
    }

    public static class BoundValue extends Value {

        private final SourceRange     sourceRange;
        private final ValueReference  reference;
        private final Type            type;

        public BoundValue(SourceRange sourceRange, ValueReference reference, Type type) {
            this.sourceRange = sourceRange;
            this.reference = reference;
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
            } else if (o instanceof BoundValue) {
                BoundValue other = (BoundValue) o;
                return Objects.equals(sourceRange, other.sourceRange)
                    && Objects.equals(reference, other.reference)
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

        @Override
        public int hashCode() {
            return Objects.hash(reference, type);
        }

        public CodeBlock reference(Scope scope) {
            return scope.getValueSignature(reference.getSymbol()).get().reference();
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + reference + " :: " + type + ")";
        }

        @Override
        public BoundValue withType(Type type) {
            return new BoundValue(sourceRange, reference, type);
        }
    }

    public static class CharLiteral extends Value {

        private final SourceRange sourceRange;
        private final char        value;

        public CharLiteral(SourceRange sourceRange, char value) {
            this.sourceRange = sourceRange;
            this.value = value;
        }

        @Override
        public <T> T accept(ValueVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof CharLiteral) {
                CharLiteral other = (CharLiteral) o;
                return Objects.equals(sourceRange, other.sourceRange)
                    && value == other.value;
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
            return sum("scotch.data.char.Char");
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + quote(value) + ")";
        }

        @Override
        public Value withType(Type type) {
            return this;
        }
    }

    public static class DoubleLiteral extends Value {

        private final SourceRange sourceRange;
        private final double      value;

        public DoubleLiteral(SourceRange sourceRange, double value) {
            this.sourceRange = sourceRange;
            this.value = value;
        }

        @Override
        public <T> T accept(ValueVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof DoubleLiteral) {
                DoubleLiteral other = (DoubleLiteral) o;
                return Objects.equals(sourceRange, other.sourceRange)
                    && value == other.value;
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
            return sum("scotch.data.double.Double");
        }

        public double getValue() {
            return value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + quote(value) + ")";
        }

        @Override
        public Value withType(Type type) {
            return this;
        }
    }

    public static class LambdaValue extends Value {

        private final Argument argument;
        private final Value body;

        public LambdaValue(Argument argument, Value body) {
            this.argument = argument;
            this.body = body;
        }

        @Override
        public <T> T accept(ValueVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof LambdaValue) {
                LambdaValue other = (LambdaValue) o;
                return Objects.equals(argument, other.argument)
                    && Objects.equals(body, other.body);
            } else {
                return false;
            }
        }

        public String getArgumentName() {
            return argument.getName();
        }

        public Value getBody() {
            return body;
        }

        @Override
        public SourceRange getSourceRange() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Type getType() {
            return Type.fn(argument.getType(), body.getType());
        }

        @Override
        public int hashCode() {
            return Objects.hash(argument, body);
        }

        @Override
        public String toString() {
            return stringify(this);
        }

        @Override
        public Value withType(Type type) {
            throw new UnsupportedOperationException();
        }
    }

    public static class FunctionValue extends Value {

        private final SourceRange    sourceRange;
        private final Symbol         symbol;
        private final List<Argument> arguments;
        private final Value          body;

        public FunctionValue(SourceRange sourceRange, Symbol symbol, List<Argument> arguments, Value body) {
            this.sourceRange = sourceRange;
            this.symbol = symbol;
            this.arguments = ImmutableList.copyOf(arguments);
            this.body = body;
        }

        @Override
        public <T> T accept(ValueVisitor<T> visitor) {
            return visitor.visit(this);
        }

        public Value curry() {
            return curry_(new ArrayDeque<>(arguments));
        }

        private Value curry_(Deque<Argument> args) {
            if (args.isEmpty()) {
                return body;
            } else {
                return new LambdaValue(args.pop(), curry_(args));
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof FunctionValue) {
                FunctionValue other = (FunctionValue) o;
                return Objects.equals(sourceRange, other.sourceRange)
                    && Objects.equals(symbol, other.symbol)
                    && Objects.equals(arguments, other.arguments)
                    && Objects.equals(body, other.body);
            } else {
                return false;
            }
        }

        public List<Argument> getArguments() {
            return arguments;
        }

        public Value getBody() {
            return body;
        }

        public ScopeReference getReference() {
            return scopeRef(symbol);
        }

        @Override
        public SourceRange getSourceRange() {
            return sourceRange;
        }

        public Symbol getSymbol() {
            return symbol;
        }

        @Override
        public Type getType() {
            List<Argument> args = new ArrayList<>(arguments);
            reverse(args);
            return args.stream()
                .map(Argument::getType)
                .reduce(body.getType(), (result, arg) -> Type.fn(arg, result));
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol, arguments, body);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + arguments + " -> " + body + ")";
        }

        public FunctionValue withArguments(List<Argument> arguments) {
            return fn(sourceRange, symbol, arguments, body);
        }

        public FunctionValue withBody(Value body) {
            return fn(sourceRange, symbol, arguments, body);
        }

        @Override
        public Value withType(Type type) {
            throw new UnsupportedOperationException();
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

        public Value boundArg(Type type) {
            return new Argument(sourceRange, symbol.getMemberName(), type);
        }

        public Value boundValue(Type type) {
            return new BoundValue(sourceRange, valueRef(symbol), type);
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

        public UnboundMethod unboundMethod(Type type) {
            return unboundMethod(sourceRange, valueRef(symbol), type);
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

    public static class Instance extends Value {

        private final SourceRange       sourceRange;
        private final InstanceReference reference;
        private final Type              type;

        public Instance(SourceRange sourceRange, InstanceReference reference, Type type) {
            this.sourceRange = sourceRange;
            this.reference = reference;
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
            } else if (o instanceof Instance) {
                Instance other = (Instance) o;
                return Objects.equals(sourceRange, other.sourceRange)
                    && Objects.equals(reference, other.reference)
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

        @Override
        public int hashCode() {
            return Objects.hash(reference, type);
        }

        public CodeBlock reference(Scope scope) {
            return reference.reference(scope);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + reference + ")";
        }

        @Override
        public Instance withType(Type type) {
            return new Instance(sourceRange, reference, type);
        }
    }

    public static class IntLiteral extends Value {

        private final SourceRange sourceRange;
        private final int         value;

        public IntLiteral(SourceRange sourceRange, int value) {
            this.sourceRange = sourceRange;
            this.value = value;
        }

        @Override
        public <T> T accept(ValueVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof IntLiteral) {
                IntLiteral other = (IntLiteral) o;
                return Objects.equals(sourceRange, other.sourceRange)
                    && value == other.value;
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
            return sum("scotch.data.int.Int");
        }

        public int getValue() {
            return value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + quote(value) + ")";
        }

        @Override
        public Value withType(Type type) {
            return this;
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

        public Value collapse() {
            if (members.size() == 1) {
                return members.get(0);
            } else {
                return this;
            }
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
        public Value unwrap() {
            return collapse().unwrap();
        }

        public Message withMembers(List<Value> members) {
            return new Message(sourceRange, members);
        }

        public Message withSourceRange(SourceRange sourceRange) {
            return new Message(sourceRange, members);
        }

        @Override
        public Value withType(Type type) {
            throw new UnsupportedOperationException();
        }
    }

    public static class Method extends Value {

        private final SourceRange    sourceRange;
        private final ValueReference reference;
        private final List<Type>     instances;
        private final Type           type;

        private Method(SourceRange sourceRange, ValueReference reference, List<Type> instances, Type type) {
            this.sourceRange = sourceRange;
            this.reference = reference;
            this.instances = ImmutableList.copyOf(instances);
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
            } else if (o instanceof Method) {
                Method other = (Method) o;
                return Objects.equals(sourceRange, other.sourceRange)
                    && Objects.equals(reference, other.reference)
                    && Objects.equals(instances, other.instances)
                    && Objects.equals(type, other.type);
            } else {
                return false;
            }
        }

        public int getInstanceCount() {
            return instances.size();
        }

        @Override
        public SourceRange getSourceRange() {
            return sourceRange;
        }

        public Symbol getSymbol() {
            return reference.getSymbol();
        }

        @Override
        public Type getType() {
            return type;
        }

        public ValueReference getReference() {
            return reference;
        }

        @Override
        public int hashCode() {
            return Objects.hash(reference, instances, type);
        }

        public CodeBlock reference(Scope scope) {
            return scope.getValueSignature(reference.getSymbol()).get().reference();
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + reference.getMemberName() + " [" + instances.stream().map(Object::toString).collect(joining(", ")) + "])";
        }

        @Override
        public Method withType(Type type) {
            return new Method(sourceRange, reference, instances, type);
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

        @Override
        public Value unwrap() {
            return withMatchers(
                matchers.stream()
                    .map(matcher -> matcher.withBody(matcher.getBody().unwrap()))
                    .collect(toList())
            );
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

    public static class StringLiteral extends Value {

        private final SourceRange sourceRange;
        private final String      value;

        public StringLiteral(SourceRange sourceRange, String value) {
            this.sourceRange = sourceRange;
            this.value = value;
        }

        @Override
        public <T> T accept(ValueVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof StringLiteral) {
                StringLiteral other = (StringLiteral) o;
                return Objects.equals(sourceRange, other.sourceRange)
                    && Objects.equals(value, other.value);
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
            return sum("scotch.data.string.String");
        }

        public String getValue() {
            return value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + quote(value) + ")";
        }

        @Override
        public Value withType(Type type) {
            return this;
        }
    }

    public static class UnboundMethod extends Value {

        private final SourceRange    sourceRange;
        private final ValueReference valueRef;
        private final Type           type;

        private UnboundMethod(SourceRange sourceRange, ValueReference valueRef, Type type) {
            this.sourceRange = sourceRange;
            this.valueRef = valueRef;
            this.type = type;
        }

        @Override
        public <T> T accept(ValueVisitor<T> visitor) {
            return visitor.visit(this);
        }

        public Value bind(Scope scope) {
            List<Type> instances = listInstanceTypes(scope.getRawValue(valueRef));
            List<Type> instanceTypes = listInstanceTypes(scope.getValue(valueRef));
            return method(sourceRange, valueRef, instances, scope.generate(getMethodType(instanceTypes)));
        }

        private List<Type> listInstanceTypes(Type valueType) {
            return valueType.getContexts().stream()
                .map(tuple -> tuple.into((type, symbol) -> Type.instance(symbol, type.simplify())))
                .collect(toList());
        }

        private Type getMethodType(List<Type> instances) {
            List<Type> reversedInstances = new ArrayList<>(instances);
            Collections.reverse(reversedInstances);
            return reversedInstances.stream().reduce(type, (left, right) -> Type.fn(right, left));
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o instanceof UnboundMethod) {
                UnboundMethod other = (UnboundMethod) o;
                return Objects.equals(sourceRange, other.sourceRange)
                    && Objects.equals(valueRef, other.valueRef)
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
            return valueRef.getSymbol();
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(valueRef, type);
        }

        @Override
        public String toString() {
            return stringify(this) + "(" + valueRef.getName() + ")";
        }

        @Override
        public Value withType(Type type) {
            return unboundMethod(sourceRange, valueRef, type);
        }
    }
}
