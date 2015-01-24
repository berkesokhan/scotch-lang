package scotch.compiler.syntax.definition;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static me.qmx.jitescript.util.CodegenUtils.ci;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static scotch.compiler.syntax.builder.BuilderUtil.require;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import me.qmx.jitescript.CodeBlock;
import me.qmx.jitescript.JiteClass;
import scotch.compiler.symbol.DataConstructorDescriptor;
import scotch.compiler.symbol.DataFieldDescriptor;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.syntax.BytecodeGenerator;
import scotch.compiler.syntax.NameAccumulator;
import scotch.compiler.syntax.NameQualifier;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.text.SourceRange;

public class DataConstructorDefinition {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceRange                      sourceRange;
    private final Symbol                           dataType;
    private final Symbol                           symbol;
    private final Map<String, DataFieldDefinition> fields;

    public DataConstructorDefinition(SourceRange sourceRange, Symbol dataType, Symbol symbol, List<DataFieldDefinition> fields) {
        this.sourceRange = sourceRange;
        this.dataType = dataType;
        this.symbol = symbol;
        this.fields = new LinkedHashMap<>();
        fields.forEach(field -> this.fields.put(field.getName(), field));
    }

    public void accumulateNames(NameAccumulator state) {
        state.defineDataConstructor(symbol, toDescriptor());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return false;
        } else if (o instanceof DataConstructorDefinition) {
            DataConstructorDefinition other = (DataConstructorDefinition) o;
            return Objects.equals(sourceRange, other.sourceRange)
                && Objects.equals(dataType, other.dataType)
                && Objects.equals(symbol, other.symbol)
                && Objects.equals(fields, other.fields);
        } else {
            return false;
        }
    }

    public void generateBytecode(BytecodeGenerator state) {
        JiteClass parentClass = state.currentClass();
        state.beginClass(state.getDataConstructorClass(symbol), parentClass.getClassName(), sourceRange);
        parentClass.addChildClass(state.currentClass());
        generateFields(state);
        generateConstructor(state, parentClass);
        if (!isNiladic()) {
            generateEquals(state);
            generateGetters(state);
            generateHashCode(state);
        }
        state.endClass();
    }

    private void generateHashCode(BytecodeGenerator state) {
        state.method("hashCode", ACC_PUBLIC, sig(int.class), new CodeBlock() {{
            ldc(fields.size());
//            anewarray(p(Object.class));
//            AtomicInteger counter = new AtomicInteger();
//            fields.values().forEach(field -> {
//                dup();
//                ldc(counter.getAndIncrement());
//                aload(0);
//                getfield(state.currentClass().getClassName(), field.getJavaName(), ci(field.getJavaType()));
//                aastore();
//            });
//            invokestatic(p(Objects.class), "hash", sig(int.class, Object[].class));
            ireturn();
        }});
    }

    private void generateEquals(BytecodeGenerator state) {
        state.method("equals", ACC_PUBLIC, sig(boolean.class, Object.class), new CodeBlock() {{
//            String className = state.currentClass().getClassName();
//            LabelNode valueCompare = new LabelNode();
//            LabelNode noCompare = new LabelNode();
//
//            // o == this
//            aload(0);
//            aload(1);
//            if_acmpne(valueCompare);
//            ldc(true);
//            ireturn();
//
//            // o instanceof {class} && values equal
//            label(valueCompare);
//            aload(1);
//            instance_of(className);
//            ifne(noCompare);
//            fields.values().forEach(field -> {
//                aload(0);
//                getfield(className, field.getJavaName(), ci(field.getJavaType()));
//                aload(1);
//                getfield(className, field.getJavaName(), ci(field.getJavaType()));
//                ifne(noCompare);
//            });
//            ldc(true);
//            ireturn();
//
//            // not this && not {class}
//            label(noCompare);
            ldc(false);
            ireturn();
        }});
    }

    private void generateFields(BytecodeGenerator state) {
        fields.values().forEach(field -> field.generateBytecode(state));
    }

    private void generateConstructor(final BytecodeGenerator state, final JiteClass parentClass) {
        Class<?>[] parameters = getParameters();
        state.method("<init>", ACC_PUBLIC, sig(void.class, parameters), new CodeBlock() {{
            aload(0);
            invokespecial(parentClass.getClassName(), "<init>", sig(void.class));
            AtomicInteger counter = new AtomicInteger(1);
            fields.values().forEach(field -> {
                int offset = counter.getAndIncrement();
                aload(0);
                aload(offset);
                putfield(state.currentClass().getClassName(), field.getJavaName(), ci(field.getJavaType()));
                counter.getAndIncrement();
            });
            voidreturn();
        }});
    }

    private void generateGetters(final BytecodeGenerator state) {
        Class<?>[] parameters = getParameters();
        AtomicInteger counter = new AtomicInteger(0);
        fields.values().forEach(field -> {
            Class<?> type = parameters[counter.getAndIncrement()];
            state.method("get" + field.getJavaName(), ACC_PUBLIC, sig(type), new CodeBlock() {{
                aload(0);
                getfield(state.currentClass().getClassName(), field.getJavaName(), ci(type));
                areturn();
            }});
        });
    }

    private Class<?>[] getParameters() {
        List<Class<?>> parameters = fields.values().stream()
            .map(DataFieldDefinition::getJavaType)
            .collect(toList());
        return parameters.toArray(new Class<?>[parameters.size()]);
    }

    public Symbol getDataType() {
        return dataType;
    }

    public List<DataFieldDefinition> getFields() {
        return new ArrayList<>(fields.values());
    }

    public SourceRange getSourceRange() {
        return sourceRange;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, fields);
    }

    public boolean isNiladic() {
        return fields.isEmpty();
    }

    public DataConstructorDefinition qualifyNames(NameQualifier state) {
        return withFields(fields.values().stream()
            .map(field -> field.qualifyNames(state))
            .collect(toList()));
    }

    private DataConstructorDefinition withFields(List<DataFieldDefinition> fields) {
        return new DataConstructorDefinition(sourceRange, dataType, symbol, fields);
    }

    public DataConstructorDescriptor toDescriptor() {
        List<DataFieldDescriptor> fieldDescriptors = fields.values().stream()
            .map(DataFieldDefinition::toDescriptor)
            .collect(toList());
        return new DataConstructorDescriptor(dataType, symbol, fieldDescriptors);
    }

    @Override
    public String toString() {
        return symbol.getSimpleName()
            + (fields.isEmpty() ? "" : " { " + fields.values().stream().map(Object::toString).collect(joining(", ")) + " }");
    }

    public static class Builder implements SyntaxBuilder<DataConstructorDefinition> {

        private Optional<SourceRange>     sourceRange;
        private Optional<Symbol>          dataType;
        private Optional<Symbol>          symbol;
        private List<DataFieldDefinition> fields;

        private Builder() {
            sourceRange = Optional.empty();
            dataType = Optional.empty();
            symbol = Optional.empty();
            fields = new ArrayList<>();
        }

        public Builder addField(DataFieldDefinition field) {
            fields.add(field);
            return this;
        }

        @Override
        public DataConstructorDefinition build() {
            return new DataConstructorDefinition(
                require(sourceRange, "Source range"),
                require(dataType, "Constructor data type"),
                require(symbol, "Constructor symbol"),
                fields
            );
        }

        public Builder withDataType(Symbol dataType) {
            this.dataType = Optional.of(dataType);
            return this;
        }

        @Override
        public Builder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }

        public Builder withSymbol(Symbol symbol) {
            this.symbol = Optional.of(symbol);
            return this;
        }
    }
}
