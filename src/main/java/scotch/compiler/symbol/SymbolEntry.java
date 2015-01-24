package scotch.compiler.symbol;

import java.util.Optional;
import scotch.compiler.symbol.DataTypeDescriptor.Builder;
import scotch.compiler.symbol.Symbol.QualifiedSymbol;
import scotch.compiler.symbol.Symbol.SymbolVisitor;
import scotch.compiler.symbol.Symbol.UnqualifiedSymbol;
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

    public abstract void defineDataType(DataTypeDescriptor dataType);

    public abstract void defineOperator(Operator operator);

    public abstract void defineSignature(Type type);

    public abstract void defineValue(Type type);

    public abstract DataTypeDescriptor getDataType();

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

    public abstract void redefineSignature(Type type);

    public abstract void redefineValue(Type type);

    public static final class ImmutableEntry extends SymbolEntry {

        private final Symbol                              symbol;
        private final Optional<Type>                      optionalValue;
        private final Optional<Operator>                  optionalOperator;
        private final Optional<Type>                      optionalType;
        private final Optional<MethodSignature>           optionalValueSignature;
        private final Optional<TypeClassDescriptor>       optionalTypeClass;
        private final Optional<Symbol>                    optionalMemberOf;
        private final Optional<DataTypeDescriptor>        optionalDataType;

        private ImmutableEntry(ImmutableEntryBuilder builder) {
            this.symbol = builder.symbol;
            this.optionalValue = builder.optionalValue;
            this.optionalOperator = builder.optionalOperator;
            this.optionalType = builder.optionalType;
            this.optionalValueSignature = builder.optionalValueSignature;
            this.optionalTypeClass = builder.optionalTypeClass;
            this.optionalMemberOf = builder.optionalMemberOf;
            this.optionalDataType = builder.dataTypeBuilder.map(Builder::build);
        }

        @Override
        public void defineDataType(DataTypeDescriptor dataType) {
            throw new IllegalStateException("Can't define data type for existing symbol " + symbol.quote());
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
        public DataTypeDescriptor getDataType() {
            return optionalDataType.orElseThrow(() -> new SymbolNotFoundException("Symbol " + symbol.quote() + " is not a data type"));
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
        public void redefineSignature(Type type) {
            throw new IllegalStateException();
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
        private       Optional<Builder>             dataTypeBuilder;

        private ImmutableEntryBuilder(Symbol symbol) {
            this.symbol = symbol;
            this.optionalValue = Optional.empty();
            this.optionalOperator = Optional.empty();
            this.optionalType = Optional.empty();
            this.optionalValueSignature = Optional.empty();
            this.optionalTypeClass = Optional.empty();
            this.optionalMemberOf = Optional.empty();
            this.dataTypeBuilder = Optional.empty();
        }

        public ImmutableEntry build() {
            return new ImmutableEntry(this);
        }

        public Builder dataType() {
            if (!dataTypeBuilder.isPresent()) {
                dataTypeBuilder = Optional.of(DataTypeDescriptor.builder(symbol));
            }
            return dataTypeBuilder.get();
        }

        public Symbol getSymbol() {
            return symbol;
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

        private final Symbol                        symbol;
        private       Optional<Type>                optionalValue;
        private       Optional<Operator>            optionalOperator;
        private       Optional<Type>                optionalType;
        private       Optional<Type>                optionalSignature;
        private       Optional<TypeClassDescriptor> optionalTypeClass;
        private       Optional<Symbol>              optionalMemberOf;
        private       Optional<DataTypeDescriptor>  optionalDataType;

        private MutableEntry(Symbol symbol) {
            this.symbol = symbol;
            this.optionalValue = Optional.empty();
            this.optionalOperator = Optional.empty();
            this.optionalType = Optional.empty();
            this.optionalSignature = Optional.empty();
            this.optionalTypeClass = Optional.empty();
            this.optionalMemberOf = Optional.empty();
            this.optionalDataType = Optional.empty();
        }

        @Override
        public void defineDataType(DataTypeDescriptor dataType) {
            if (optionalDataType.isPresent()) {
                throw new IllegalStateException("Data type has already been defined for " + symbol.quote());
            } else {
                this.optionalDataType = Optional.of(dataType);
            }
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
        public DataTypeDescriptor getDataType() {
            return optionalDataType.orElseThrow(() -> new SymbolNotFoundException("Symbol " + symbol.quote() + " is not a data type"));
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
            return optionalSignature;
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
            return symbol.accept(new SymbolVisitor<MethodSignature>() {
                @Override
                public MethodSignature visit(QualifiedSymbol symbol) {
                    return optionalValue.map(symbol::getMethodSignature).orElseThrow(UnsupportedOperationException::new); // TODO
                }

                @Override
                public MethodSignature visit(UnqualifiedSymbol symbol) {
                    throw new UnsupportedOperationException(); // TODO
                }
            });
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
        public void redefineSignature(Type type) {
            if (optionalSignature.isPresent()) {
                optionalSignature = Optional.of(type);
            } else {
                throw new SymbolNotFoundException("Can't redefine non-existent signature " + symbol.quote());
            }
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
