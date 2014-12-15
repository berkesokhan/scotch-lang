package scotch.compiler.symbol;

import java.util.Optional;
import scotch.compiler.symbol.exception.SymbolNotFoundException;

public abstract class SymbolEntry {

    public static ImmutableEntryBuilder immutableEntry(Symbol symbol) {
        return new ImmutableEntryBuilder(symbol);
    }

    public static SymbolEntry mutableEntry(Symbol symbol) {
        return new MutableEntry(symbol);
    }

    private SymbolEntry() {
        // intentionally empty
    }

    public abstract void defineOperator(Operator operator);

    public abstract void defineSignature(Type type);

    public abstract void defineValue(Type type);

    public abstract Symbol getMemberOf();

    public abstract Operator getOperator();

    public abstract Optional<Type> getSignature();

    public abstract Symbol getSymbol();

    public abstract Type getType();

    public abstract TypeClassDescriptor getTypeClass();

    public abstract Type getValue();

    public abstract MethodSignature getValueSignature();

    public abstract boolean isMember();

    public abstract boolean isOperator();

    public abstract void redefineValue(Type type);

    public static final class ImmutableEntry extends SymbolEntry {

        private final Symbol                        symbol;
        private final Optional<Type>                optionalValue;
        private final Optional<Operator>            optionalOperator;
        private final Optional<Type>                optionalType;
        private final Optional<MethodSignature>     optionalValueSignature;
        private final Optional<TypeClassDescriptor> optionalTypeClass;
        private final Optional<Symbol>              optionalMemberOf;

        private ImmutableEntry(ImmutableEntryBuilder builder) {
            this.symbol = builder.symbol;
            this.optionalValue = builder.optionalValue;
            this.optionalOperator = builder.optionalOperator;
            this.optionalType = builder.optionalType;
            this.optionalValueSignature = builder.optionalValueSignature;
            this.optionalTypeClass = builder.optionalTypeClass;
            this.optionalMemberOf = builder.optionalMemberOf;
        }

        @Override
        public void defineOperator(Operator operator) {
            throw new IllegalStateException("Can't define operator for existing symbol " + symbol.quote());
        }

        @Override
        public void defineSignature(Type type) {
            throw new IllegalStateException("Can't define signature for existing symbol " + symbol.quote());
        }

        @Override
        public void defineValue(Type type) {
            throw new IllegalStateException("Can't define value for existing symbol " + symbol.quote());
        }

        @Override
        public Symbol getMemberOf() {
            return optionalMemberOf.orElseThrow(() -> new SymbolNotFoundException("Symbol " + symbol.quote() + " is not a class member"));
        }

        @Override
        public Operator getOperator() {
            return optionalOperator.orElseThrow(() -> new SymbolNotFoundException("Symbol " + symbol.quote() + " is not an operator"));
        }

        @Override
        public Optional<Type> getSignature() {
            return optionalValue;
        }

        @Override
        public Symbol getSymbol() {
            return symbol;
        }

        @Override
        public Type getType() {
            return optionalType.orElseThrow(() -> new SymbolNotFoundException("Symbol " + symbol.quote() + " is not a type"));
        }

        @Override
        public TypeClassDescriptor getTypeClass() {
            return optionalTypeClass.orElseThrow(() -> new SymbolNotFoundException("Symbol " + symbol.quote() + " is not a type class"));
        }

        @Override
        public Type getValue() {
            return optionalValue.orElseThrow(() -> new SymbolNotFoundException("Symbol " + symbol.quote() + " is not a value"));
        }

        @Override
        public MethodSignature getValueSignature() {
            return optionalValueSignature.orElseThrow(() -> new SymbolNotFoundException("Symbol " + symbol.quote() + " has no value signature"));
        }

        @Override
        public boolean isMember() {
            return optionalMemberOf.isPresent();
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

        private final Symbol                        symbol;
        private       Optional<Type>                optionalValue;
        private       Optional<Operator>            optionalOperator;
        private       Optional<Type>                optionalType;
        private       Optional<MethodSignature>     optionalValueSignature;
        private       Optional<TypeClassDescriptor> optionalTypeClass;
        private       Optional<Symbol>              optionalMemberOf;

        private ImmutableEntryBuilder(Symbol symbol) {
            this.symbol = symbol;
            this.optionalValue = Optional.empty();
            this.optionalOperator = Optional.empty();
            this.optionalType = Optional.empty();
            this.optionalValueSignature = Optional.empty();
            this.optionalTypeClass = Optional.empty();
            this.optionalMemberOf = Optional.empty();
        }

        public ImmutableEntry build() {
            return new ImmutableEntry(this);
        }

        public ImmutableEntryBuilder withMemberOf(Symbol memberOf) {
            optionalMemberOf = Optional.of(memberOf);
            return this;
        }

        public ImmutableEntryBuilder withOperator(Operator operator) {
            optionalOperator = Optional.of(operator);
            return this;
        }

        public ImmutableEntryBuilder withType(Type type) {
            optionalType = Optional.of(type);
            return this;
        }

        public ImmutableEntryBuilder withTypeClass(TypeClassDescriptor typeClass) {
            optionalTypeClass = Optional.of(typeClass);
            return this;
        }

        public ImmutableEntryBuilder withValue(Type type) {
            optionalValue = Optional.of(type);
            return this;
        }

        public ImmutableEntryBuilder withValueSignature(MethodSignature valueSignature) {
            optionalValueSignature = Optional.of(valueSignature);
            return this;
        }
    }

    public static final class MutableEntry extends SymbolEntry {

        private final Symbol             symbol;
        private       Optional<Type>     optionalValue;
        private       Optional<Operator> optionalOperator;
        private       Optional<Type>     optionalSignature;

        private MutableEntry(Symbol symbol) {
            this.symbol = symbol;
            this.optionalValue = Optional.empty();
            this.optionalOperator = Optional.empty();
            this.optionalSignature = Optional.empty();
        }

        @Override
        public void defineOperator(Operator operator) {
            if (optionalOperator.isPresent()) {
                throw new IllegalStateException("Operator has already been defined for " + symbol.quote());
            } else {
                optionalOperator = Optional.of(operator);
            }
        }

        @Override
        public void defineSignature(Type type) {
            if (optionalValue.isPresent()) {
                throw new IllegalStateException("Value has already been defined for " + symbol.quote());
            } else if (optionalSignature.isPresent()) {
                throw new IllegalStateException("Signature has already been defined for " + symbol.quote());
            } else {
                optionalSignature = Optional.of(type);
            }
        }

        @Override
        public void defineValue(Type type) {
            if (optionalValue.isPresent()) {
                throw new IllegalStateException("Value has already been defined for " + symbol.quote());
            } else {
                optionalValue = Optional.of(type);
            }
        }

        @Override
        public Symbol getMemberOf() {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public Operator getOperator() {
            return optionalOperator.orElseThrow(() -> new SymbolNotFoundException("Symbol " + symbol.quote() + " is not an operator"));
        }

        @Override
        public Optional<Type> getSignature() {
            return optionalSignature;
        }

        @Override
        public Symbol getSymbol() {
            return symbol;
        }

        @Override
        public Type getType() {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public TypeClassDescriptor getTypeClass() {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public Type getValue() {
            return optionalValue.orElseThrow(() -> new SymbolNotFoundException("Symbol " + symbol.quote() + " is not a value"));
        }

        @Override
        public MethodSignature getValueSignature() {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public boolean isMember() {
            return false; // TODO
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
                throw new SymbolNotFoundException("Can't redefine non-existent value " + symbol.quote());
            }
        }
    }
}
