package scotch.compiler.symbol;

import static org.apache.commons.lang.StringUtils.capitalize;

import java.util.Optional;
import scotch.compiler.symbol.exception.SymbolNotFoundException;
import scotch.compiler.symbol.type.Type;

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

    public abstract void defineDataConstructor(DataConstructorDescriptor dataConstructor);

    public abstract void defineDataType(DataTypeDescriptor dataType);

    public abstract void defineOperator(Operator operator);

    public abstract void defineSignature(Type type);

    public abstract void defineValue(Type type, MethodSignature valueMethod);

    public Optional<DataConstructorDescriptor> getConstructor(Symbol symbol) {
        return getDataType().getConstructor(symbol);
    }

    public abstract DataConstructorDescriptor getDataConstructor();

    public abstract DataTypeDescriptor getDataType();

    public abstract Symbol getMemberOf();

    public abstract Operator getOperator();

    public abstract Optional<Type> getSignature();

    public abstract Symbol getSymbol();

    public abstract Type getType();

    public abstract TypeClassDescriptor getTypeClass();

    public abstract Type getValue();

    public abstract MethodSignature getValueMethod();

    public abstract boolean isDataConstructor();

    public abstract boolean isMember();

    public abstract boolean isOperator();

    public abstract void redefineSignature(Type type);

    public abstract void redefineValue(Type type, MethodSignature valueMethod);

    protected <T> T get(Optional<T> optional, String kind) {
        return optional.orElseThrow(() -> new SymbolNotFoundException("Symbol " + getSymbol().quote() + " is not " + kind));
    }

    public static final class ImmutableEntry extends SymbolEntry {

        private final Symbol                              symbol;
        private final Optional<Type>                      optionalValue;
        private final Optional<Operator>                  optionalOperator;
        private final Optional<Type>                      optionalType;
        private final Optional<MethodSignature>           optionalValueMethod;
        private final Optional<TypeClassDescriptor>       optionalTypeClass;
        private final Optional<Symbol>                    optionalMemberOf;
        private final Optional<DataTypeDescriptor>        optionalDataType;
        private final Optional<DataConstructorDescriptor> optionalDataConstructor;

        private ImmutableEntry(ImmutableEntryBuilder builder) {
            this.symbol = builder.symbol;
            optionalValue = builder.optionalValue;
            optionalOperator = builder.optionalOperator;
            optionalType = builder.optionalType;
            optionalValueMethod = builder.optionalValueMethod;
            optionalTypeClass = builder.optionalTypeClass;
            optionalMemberOf = builder.optionalMemberOf;
            optionalDataType = builder.dataTypeBuilder.map(DataTypeDescriptor.Builder::build);
            optionalDataConstructor = builder.dataConstructorBuilder.map(DataConstructorDescriptor.Builder::build);
        }

        @Override
        public void defineDataConstructor(DataConstructorDescriptor dataConstructor) {
            throw existingSymbol("data constructor");
        }

        @Override
        public void defineDataType(DataTypeDescriptor dataType) {
            throw existingSymbol("data type");
        }

        @Override
        public void defineOperator(Operator operator) {
            throw existingSymbol("operator");
        }

        @Override
        public void defineSignature(Type type) {
            throw existingSymbol("signature");
        }

        @Override
        public void defineValue(Type type, MethodSignature valueMethod) {
            throw existingSymbol("value");
        }

        @Override
        public DataConstructorDescriptor getDataConstructor() {
            return get(optionalDataConstructor, "a data constructor");
        }

        @Override
        public DataTypeDescriptor getDataType() {
            return get(optionalDataType, "a data type");
        }

        @Override
        public Symbol getMemberOf() {
            return get(optionalMemberOf, "a class member");
        }

        @Override
        public Operator getOperator() {
            return get(optionalOperator, "an operator");
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
            return get(optionalType, "a type");
        }

        @Override
        public TypeClassDescriptor getTypeClass() {
            return get(optionalTypeClass, "a type class");
        }

        @Override
        public Type getValue() {
            return get(optionalValue, "a value");
        }

        @Override
        public MethodSignature getValueMethod() {
            return get(optionalValueMethod, "a value");
        }

        @Override
        public boolean isDataConstructor() {
            return optionalDataConstructor.isPresent();
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
            throw existingSymbol("value");
        }

        @Override
        public void redefineValue(Type type, MethodSignature valueMethod) {
            throw existingSymbol("value");
        }

        private IllegalStateException existingSymbol(String kind) {
            return new IllegalStateException("Can't define " + kind + " for existing symbol " + symbol.quote());
        }
    }

    public static final class ImmutableEntryBuilder {

        private final Symbol symbol;
        private Optional<Type>                              optionalValue          = Optional.empty();
        private Optional<Operator>                          optionalOperator       = Optional.empty();
        private Optional<Type>                              optionalType           = Optional.empty();
        private Optional<TypeClassDescriptor>               optionalTypeClass      = Optional.empty();
        private Optional<Symbol>                            optionalMemberOf       = Optional.empty();
        private Optional<DataTypeDescriptor.Builder>        dataTypeBuilder        = Optional.empty();
        private Optional<MethodSignature>                   optionalValueMethod    = Optional.empty();
        private Optional<DataConstructorDescriptor.Builder> dataConstructorBuilder = Optional.empty();

        private ImmutableEntryBuilder(Symbol symbol) {
            this.symbol = symbol;
        }

        public ImmutableEntry build() {
            return new ImmutableEntry(this);
        }

        public DataConstructorDescriptor.Builder dataConstructor(Symbol dataType) {
            if (!dataConstructorBuilder.isPresent()) {
                dataConstructorBuilder = Optional.of(DataConstructorDescriptor.builder(dataType, symbol));
            }
            return dataConstructorBuilder.get();
        }

        public DataTypeDescriptor.Builder dataType() {
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

        public ImmutableEntryBuilder withValueType(Type type) {
            optionalValue = Optional.of(type);
            return this;
        }

        public ImmutableEntryBuilder withValueMethod(MethodSignature valueMethod) {
            optionalValueMethod = Optional.of(valueMethod);
            return this;
        }
    }

    public static final class MutableEntry extends SymbolEntry {

        private final Symbol symbol;
        private Optional<Type>                      optionalValue           = Optional.empty();
        private Optional<Operator>                  optionalOperator        = Optional.empty();
        private Optional<Type>                      optionalType            = Optional.empty();
        private Optional<Type>                      optionalSignature       = Optional.empty();
        private Optional<TypeClassDescriptor>       optionalTypeClass       = Optional.empty();
        private Optional<Symbol>                    optionalMemberOf        = Optional.empty();
        private Optional<DataTypeDescriptor>        optionalDataType        = Optional.empty();
        private Optional<DataConstructorDescriptor> optionalDataConstructor = Optional.empty();
        private Optional<MethodSignature>           optionalValueMethod     = Optional.empty();

        private MutableEntry(Symbol symbol) {
            this.symbol = symbol;
        }

        @Override
        public void defineDataConstructor(DataConstructorDescriptor dataConstructor) {
            if (optionalDataConstructor.isPresent()) {
                throw alreadyDefined("data constructor");
            } else {
                optionalDataConstructor = Optional.of(dataConstructor);
            }
        }

        @Override
        public void defineDataType(DataTypeDescriptor dataType) {
            if (optionalDataType.isPresent()) {
                throw alreadyDefined("data type");
            } else {
                optionalDataType = Optional.of(dataType);
            }
        }

        @Override
        public void defineOperator(Operator operator) {
            if (optionalOperator.isPresent()) {
                throw alreadyDefined("operator");
            } else {
                optionalOperator = Optional.of(operator);
            }
        }

        @Override
        public void defineSignature(Type type) {
            if (optionalValue.isPresent()) {
                throw alreadyDefined("value");
            } else if (optionalSignature.isPresent()) {
                throw alreadyDefined("signature");
            } else {
                optionalSignature = Optional.of(type);
            }
        }

        @Override
        public void defineValue(Type type, MethodSignature valueMethod) {
            if (optionalValue.isPresent()) {
                throw alreadyDefined("value");
            } else {
                optionalValue = Optional.of(type);
            }
        }

        @Override
        public DataConstructorDescriptor getDataConstructor() {
            return get(optionalDataConstructor, "a data constructor");
        }

        @Override
        public DataTypeDescriptor getDataType() {
            return get(optionalDataType, "a data type");
        }

        @Override
        public Symbol getMemberOf() {
            return get(optionalMemberOf, "a class member");
        }

        @Override
        public Operator getOperator() {
            return get(optionalOperator, "an operator");
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
            return get(optionalType, "a type");
        }

        @Override
        public TypeClassDescriptor getTypeClass() {
            return get(optionalTypeClass, " a type class");
        }

        @Override
        public Type getValue() {
            return get(optionalValue, "a value");
        }

        @Override
        public MethodSignature getValueMethod() {
            return get(optionalValueMethod, " a value");
        }

        @Override
        public boolean isDataConstructor() {
            return optionalDataConstructor.isPresent();
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
        public void redefineValue(Type type, MethodSignature valueMethod) {
            if (optionalValue.isPresent()) {
                optionalValue = Optional.of(type);
                optionalValueMethod = Optional.of(valueMethod);
            } else {
                throw new SymbolNotFoundException("Can't redefine non-existent value " + symbol.quote());
            }
        }

        private IllegalStateException alreadyDefined(String thing) {
            return new IllegalStateException(capitalize(thing) + " is already defined for " + symbol.quote());
        }
    }
}
