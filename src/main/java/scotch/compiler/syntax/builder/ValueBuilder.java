package scotch.compiler.syntax.builder;

import static scotch.compiler.syntax.Value.id;
import static scotch.compiler.syntax.Value.literal;
import static scotch.compiler.syntax.Value.message;
import static scotch.compiler.syntax.builder.BuilderUtil.require;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.syntax.Value;
import scotch.compiler.syntax.Value.Identifier;
import scotch.compiler.syntax.Value.LiteralValue;
import scotch.compiler.syntax.Value.Message;
import scotch.compiler.text.SourceRange;

public abstract class ValueBuilder<T extends Value> implements SyntaxBuilder<T> {

    public static IdentifierBuilder idBuilder() {
        return new IdentifierBuilder();
    }

    public static LiteralBuilder literalBuilder() {
        return new LiteralBuilder();
    }

    public static MessageBuilder messageBuilder() {
        return new MessageBuilder();
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

    public static class LiteralBuilder extends ValueBuilder<LiteralValue> {

        private Optional<Object>      value       = Optional.empty();
        private Optional<Type>        type        = Optional.empty();
        private Optional<SourceRange> sourceRange = Optional.empty();

        private LiteralBuilder() {
            // intentionally empty
        }

        @Override
        public LiteralValue build() {
            return literal(
                require(sourceRange, "Source range"),
                require(value, "Literal value"),
                require(type, "Literal type")
            );
        }

        @Override
        public LiteralBuilder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }

        public LiteralBuilder withType(Type type) {
            this.type = Optional.of(type);
            return this;
        }

        public LiteralBuilder withValue(Object value) {
            this.value = Optional.of(value);
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
