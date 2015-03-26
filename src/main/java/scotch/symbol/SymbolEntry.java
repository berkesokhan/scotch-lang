package scotch.symbol;

import static org.apache.commons.lang.StringUtils.capitalize;

import java.util.Optional;
import scotch.symbol.descriptor.DataConstructorDescriptor;
import scotch.symbol.descriptor.DataTypeDescriptor;
import scotch.symbol.descriptor.TypeClassDescriptor;
import scotch.symbol.type.Type;

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
        return getDataType().flatMap(dataType -> dataType.getConstructor(symbol));
    }

    public abstract Optional<DataConstructorDescriptor> getDataConstructor();

    public abstract Optional<DataTypeDescriptor> getDataType();

    public abstract Optional<Symbol> getMemberOf();

    public abstract Optional<Operator> getOperator();

    public abstract Optional<Type> getSignature();

    public abstract Symbol getSymbol();

    public abstract Optional<Type> getType();

    public abstract Optional<TypeClassDescriptor> getTypeClass();

    public abstract Optional<Type> getValue();

    public abstract Optional<MethodSignature> getValueMethod();

    public abstract boolean isDataConstructor();

    public abstract boolean isMember();

    public abstract boolean isOperator();

    public abstract void redefineDataConstructor(DataConstructorDescriptor descriptor);

    public abstract void redefineDataType(DataTypeDescriptor descriptor);

    public abstract void redefineSignature(Type type);

    public abstract void redefineValue(Type type, MethodSignature valueMethod);

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
        public Optional<DataConstructorDescriptor> getDataConstructor() {
            return optionalDataConstructor;
        }

        @Override
        public Optional<DataTypeDescriptor> getDataType() {
            return optionalDataType;
        }

        @Override
        public Optional<Symbol> getMemberOf() {
            return optionalMemberOf;
        }

        @Override
        public Optional<Operator> getOperator() {
            return optionalOperator;
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
        public Optional<Type> getType() {
            return optionalType;
        }

        @Override
        public Optional<TypeClassDescriptor> getTypeClass() {
            return optionalTypeClass;
        }

        @Override
        public Optional<Type> getValue() {
            return optionalValue;
        }

        @Override
        public Optional<MethodSignature> getValueMethod() {
            return optionalValueMethod;
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
        public void redefineDataConstructor(DataConstructorDescriptor descriptor) {
            throw existingSymbol("data constructor");
        }

        @Override
        public void redefineDataType(DataTypeDescriptor descriptor) {
            throw existingSymbol("data type");
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

        public DataConstructorDescriptor.Builder dataConstructor(int ordinal, Symbol dataType) {
            if (!dataConstructorBuilder.isPresent()) {
                dataConstructorBuilder = Optional.of(DataConstructorDescriptor.builder(ordinal, dataType, symbol));
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
        public Optional<DataConstructorDescriptor> getDataConstructor() {
            return optionalDataConstructor;
        }

        @Override
        public Optional<DataTypeDescriptor> getDataType() {
            return optionalDataType;
        }

        @Override
        public Optional<Symbol> getMemberOf() {
            return optionalMemberOf;
        }

        @Override
        public Optional<Operator> getOperator() {
            return optionalOperator;
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
        public Optional<Type> getType() {
            return optionalType;
        }

        @Override
        public Optional<TypeClassDescriptor> getTypeClass() {
            return optionalTypeClass;
        }

        @Override
        public Optional<Type> getValue() {
            return optionalValue;
        }

        @Override
        public Optional<MethodSignature> getValueMethod() {
            return optionalValueMethod;
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
        public void redefineDataConstructor(DataConstructorDescriptor descriptor) {
            if (optionalDataConstructor.isPresent()) {
                optionalDataConstructor = Optional.of(descriptor);
            } else {
                throw new IllegalStateException("Can't redefine non-existent data constructor " + symbol.quote());
            }
        }

        @Override
        public void redefineDataType(DataTypeDescriptor descriptor) {
            if (optionalDataType.isPresent()) {
                optionalDataType = Optional.of(descriptor);
            } else {
                throw new IllegalStateException("Can't redefine non-existent data type " + symbol.quote());
            }
        }

        @Override
        public void redefineSignature(Type type) {
            if (optionalSignature.isPresent()) {
                optionalSignature = Optional.of(type);
            } else {
                throw new IllegalStateException("Can't redefine non-existent signature " + symbol.quote());
            }
        }

        @Override
        public void redefineValue(Type type, MethodSignature valueMethod) {
            if (optionalValue.isPresent()) {
                optionalValue = Optional.of(type);
                optionalValueMethod = Optional.of(valueMethod);
            } else {
                throw new IllegalStateException("Can't redefine non-existent value " + symbol.quote());
            }
        }

        private IllegalStateException alreadyDefined(String thing) {
            return new IllegalStateException(capitalize(thing) + " is already defined for " + symbol.quote());
        }
    }
}
