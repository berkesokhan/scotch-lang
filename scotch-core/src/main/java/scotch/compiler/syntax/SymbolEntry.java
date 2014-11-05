package scotch.compiler.syntax;

import static scotch.compiler.syntax.Scope.symbolNotFound;

import java.util.Optional;

public abstract class SymbolEntry {

    public static SymbolEntry mutableEntry(Symbol symbol) {
        return new MutableEntry(symbol);
    }

    public static ImmutableEntryBuilder immutableEntry(Symbol symbol) {
        return new ImmutableEntryBuilder(symbol);
    }

    public abstract void defineValue(Type type);

    public abstract void defineOperator(Operator operator);

    public abstract Operator getOperator();

    public abstract Symbol getSymbol();

    public abstract Type getValue();

    public abstract boolean isOperator();

    public abstract void redefineValue(Type type);

    public static final class ImmutableEntry extends SymbolEntry {

        private final Symbol             symbol;
        private final Optional<Type>     optionalValue;
        private final Optional<Operator> optionalOperator;

        public ImmutableEntry(Symbol symbol, Optional<Type> optionalValue, Optional<Operator> optionalOperator) {
            this.symbol = symbol;
            this.optionalValue = optionalValue;
            this.optionalOperator = optionalOperator;
        }

        @Override
        public void defineValue(Type type) {
            throw new IllegalStateException();
        }

        @Override
        public void defineOperator(Operator operator) {
            throw new IllegalStateException();
        }

        @Override
        public Operator getOperator() {
            return optionalOperator.orElseThrow(() -> symbolNotFound(symbol));
        }

        @Override
        public Symbol getSymbol() {
            return symbol;
        }

        @Override
        public Type getValue() {
            return optionalValue.orElseThrow(() -> symbolNotFound(symbol));
        }

        @Override
        public boolean isOperator() {
            return optionalOperator.isPresent();
        }

        @Override
        public void redefineValue(Type type) {
            throw new IllegalStateException();
        }
    }

    public static final class ImmutableEntryBuilder {

        private final Symbol             symbol;
        private       Optional<Type>     optionalValue;
        private       Optional<Operator> optionalOperator;

        public ImmutableEntryBuilder(Symbol symbol) {
            this.symbol = symbol;
            this.optionalValue = Optional.empty();
            this.optionalOperator = Optional.empty();
        }

        public ImmutableEntryBuilder withOperator(Operator operator) {
            optionalOperator = Optional.of(operator);
            return this;
        }

        public ImmutableEntryBuilder withValue(Type type) {
            optionalValue = Optional.of(type);
            return this;
        }

        public ImmutableEntry build() {
            return new ImmutableEntry(symbol, optionalValue, optionalOperator);
        }
    }

    public static final class MutableEntry extends SymbolEntry {

        private final Symbol             symbol;
        private       Optional<Type>     optionalValue;
        private       Optional<Operator> optionalOperator;

        private MutableEntry(Symbol symbol) {
            this.symbol = symbol;
            this.optionalValue = Optional.empty();
            this.optionalOperator = Optional.empty();
        }

        @Override
        public void defineOperator(Operator operator) {
            if (optionalOperator.isPresent()) {
                throw new UnsupportedOperationException(); // TODO
            } else {
                optionalOperator = Optional.of(operator);
            }
        }

        @Override
        public void defineValue(Type type) {
            if (optionalValue.isPresent()) {
                throw new UnsupportedOperationException(); // TODO
            } else {
                optionalValue = Optional.of(type);
            }
        }

        @Override
        public Operator getOperator() {
            return optionalOperator.orElseThrow(() -> symbolNotFound(symbol));
        }

        @Override
        public Symbol getSymbol() {
            return symbol;
        }

        @Override
        public Type getValue() {
            return optionalValue.orElseThrow(() -> symbolNotFound(symbol));
        }

        @Override
        public boolean isOperator() {
            return optionalOperator.isPresent();
        }

        @Override
        public void redefineValue(Type type) {
            if (optionalValue.isPresent()) {
                optionalValue = Optional.of(type);
            } else {
                throw symbolNotFound(symbol);
            }
        }
    }
}
