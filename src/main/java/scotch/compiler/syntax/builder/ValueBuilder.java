package scotch.compiler.syntax.builder;

import static scotch.compiler.syntax.Value.arg;
import static scotch.compiler.syntax.Value.fn;
import static scotch.compiler.syntax.Value.id;
import static scotch.compiler.syntax.Value.literal;
import static scotch.compiler.syntax.Value.message;
import static scotch.compiler.syntax.builder.BuilderUtil.require;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.syntax.Value;
import scotch.compiler.syntax.Value.Argument;
import scotch.compiler.syntax.Value.FunctionValue;
import scotch.compiler.syntax.Value.Identifier;
import scotch.compiler.syntax.Value.Message;
import scotch.compiler.syntax.Value.ValueVisitor;
import scotch.compiler.text.SourceRange;

public abstract class ValueBuilder<T extends Value> implements SyntaxBuilder<T> {

    public static ArgumentBuilder argumentBuilder() {
        return new ArgumentBuilder();
    }

    public static FunctionBuilder functionBuilder() {
        return new FunctionBuilder();
    }

    public static IdentifierBuilder idBuilder() {
        return new IdentifierBuilder();
    }

    public static LiteralBuilder literalBuilder() {
        return new LiteralBuilder();
    }

    public static MessageBuilder messageBuilder() {
        return new MessageBuilder();
    }

    private ValueBuilder() {
        // intentionally empty
    }

    public static class ArgumentBuilder extends ValueBuilder<Argument> {

        private Optional<String>      name;
        private Optional<Type>        type;
        private Optional<SourceRange> sourceRange;

        private ArgumentBuilder() {
            // intentionally empty
        }

        @Override
        public Argument build() {
            return arg(
                require(sourceRange, "Source range"),
                require(name, "Argument name"),
                require(type, "Argument type")
            );
        }

        public ArgumentBuilder withName(String name) {
            this.name = Optional.of(name);
            return this;
        }

        @Override
        public ArgumentBuilder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }

        public ArgumentBuilder withType(Type type) {
            this.type = Optional.of(type);
            return this;
        }
    }

    public static class FunctionBuilder extends ValueBuilder<FunctionValue> {

        private Optional<Symbol>         symbol;
        private Optional<List<Argument>> arguments;
        private Optional<Value>          body;
        private Optional<SourceRange>    sourceRange;

        private FunctionBuilder() {
            symbol = Optional.empty();
            arguments = Optional.empty();
            body = Optional.empty();
            sourceRange = Optional.empty();
        }

        @Override
        public FunctionValue build() {
            return fn(
                require(sourceRange, "Source range"),
                require(symbol, "Function symbol"),
                require(arguments, "Function arguments"),
                require(body, "Function body").accept(new ValueVisitor<Value>() {
                    @Override
                    public Value visit(Message message) {
                        return message.collapse();
                    }

                    @Override
                    public Value visitOtherwise(Value value) {
                        return value;
                    }
                })
            );
        }

        public FunctionBuilder withArguments(List<Argument> arguments) {
            this.arguments = Optional.of(arguments);
            return this;
        }

        public FunctionBuilder withBody(Value body) {
            this.body = Optional.of(body);
            return this;
        }

        @Override
        public FunctionBuilder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }

        public void withSymbol(Symbol symbol) {
            this.symbol = Optional.of(symbol);
        }
    }

    public static class IdentifierBuilder extends ValueBuilder<Identifier> {

        private Optional<Symbol>      symbol      = Optional.empty();
        private Optional<Type>        type        = Optional.empty();
        private Optional<SourceRange> sourceRange = Optional.empty();

        private IdentifierBuilder() {
            // intentionally empty
        }

        @Override
        public Identifier build() {
            return id(
                require(sourceRange, "Source range"),
                require(symbol, "Identifier symbol"),
                require(type, "Identifier type")
            );
        }

        @Override
        public IdentifierBuilder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }

        public IdentifierBuilder withSymbol(Symbol symbol) {
            this.symbol = Optional.of(symbol);
            return this;
        }

        public IdentifierBuilder withType(Type type) {
            this.type = Optional.of(type);
            return this;
        }
    }

    public static class LiteralBuilder extends ValueBuilder<Value> {

        private Optional<Function<SourceRange, Value>> constructor;
        private Optional<SourceRange>                  sourceRange;

        private LiteralBuilder() {
            constructor = Optional.empty();
        }

        @Override
        public Value build() {
            return require(constructor, "Literal value").apply(require(sourceRange, "Source range"));
        }

        @Override
        public SyntaxBuilder<Value> withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }

        public LiteralBuilder withValue(char value) {
            constructor = Optional.of(sourceRange -> literal(sourceRange, value));
            return this;
        }

        public LiteralBuilder withValue(double value) {
            constructor = Optional.of(sourceRange -> literal(sourceRange, value));
            return this;
        }

        public LiteralBuilder withValue(int value) {
            constructor = Optional.of(sourceRange -> literal(sourceRange, value));
            return this;
        }

        public LiteralBuilder withValue(String value) {
            constructor = Optional.of(sourceRange -> literal(sourceRange, value));
            return this;
        }

        public LiteralBuilder withValue(boolean value) {
            constructor = Optional.of(sourceRange -> literal(sourceRange, value));
            return this;
        }
    }

    public static class MessageBuilder extends ValueBuilder<Message> {

        private final List<Value>           members     = new ArrayList<>();
        private       Optional<SourceRange> sourceRange = Optional.empty();

        private MessageBuilder() {
            // intentionally empty
        }

        @Override
        public Message build() {
            return message(
                require(sourceRange, "Source range"),
                members
            );
        }

        public MessageBuilder withMember(Value member) {
            members.add(member);
            return this;
        }

        @Override
        public MessageBuilder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }
    }
}
