package scotch.compiler.syntax.definition;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static me.qmx.jitescript.util.CodegenUtils.ci;
import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static org.apache.commons.lang.StringUtils.capitalize;
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
import org.objectweb.asm.tree.LabelNode;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.NameQualifier;
import scotch.compiler.symbol.DataConstructorDescriptor;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.text.SourceRange;
import scotch.runtime.Callable;

public class DataConstructorDefinition {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceRange                      sourceRange;
    private final Symbol                           dataType;
    private final Symbol                           symbol;
    private final Map<String, DataFieldDefinition> fields;

    private DataConstructorDefinition(SourceRange sourceRange, Symbol dataType, Symbol symbol, List<DataFieldDefinition> fields) {
        this.sourceRange = sourceRange;
        this.dataType = dataType;
        this.symbol = symbol;
        this.fields = new LinkedHashMap<>();
        fields.forEach(field -> this.fields.put(field.getName(), field));
    }

    public void accumulateNames(NameAccumulator state) {
        state.defineDataConstructor(symbol, getDescriptor());
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

    public Symbol getDataType() {
        return dataType;
    }

    public DataConstructorDescriptor getDescriptor() {
        return DataConstructorDescriptor.builder(dataType, symbol)
            .withFields(fields.values().stream()
                .map(DataFieldDefinition::getDescriptor)
                .collect(toList()))
            .build();
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

    @Override
    public String toString() {
        return symbol.getSimpleName()
            + (fields.isEmpty() ? "" : " { " + fields.values().stream().map(Object::toString).collect(joining(", ")) + " }");
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

    private void generateEquals(BytecodeGenerator state) {
        state.method("equals", ACC_PUBLIC, sig(boolean.class, Object.class), new CodeBlock() {{
            String className = state.currentClass().getClassName();
            LabelNode equal = new LabelNode();
            LabelNode valueCompare = new LabelNode();
            LabelNode notEqual = new LabelNode();

            // o == this
            aload(0);
            aload(1);
            if_acmpne(valueCompare);
            go_to(equal);

            // o instanceof {class} && values equal
            label(valueCompare);
            aload(1);
            instance_of(className);
            ifeq(notEqual);
            if (!fields.isEmpty()) {
                aload(1);
                checkcast(className);
                astore(2);
                fields.values().forEach(field -> {
                    aload(0);
                    getfield(className, field.getJavaName(), ci(field.getJavaType()));
                    invokeinterface(p(Callable.class), "call", sig(Object.class));
                    aload(2);
                    checkcast(className);
                    getfield(className, field.getJavaName(), ci(field.getJavaType()));
                    invokeinterface(p(Callable.class), "call", sig(Object.class));
                    invokestatic(p(Objects.class), "equals", sig(boolean.class, Object.class, Object.class));
                    ifeq(notEqual);
                });
            }

            label(equal);
            iconst_1();
            ireturn();

            // not this && not {class}
            label(notEqual);
            iconst_0();
            ireturn();
        }});
    }

    private void generateFields(BytecodeGenerator state) {
        fields.values().forEach(field -> field.generateBytecode(state));
    }

    private void generateGetters(final BytecodeGenerator state) {
        Class<?>[] parameters = getParameters();
        AtomicInteger counter = new AtomicInteger(0);
        fields.values().forEach(field -> {
            Class<?> type = parameters[counter.getAndIncrement()];
            state.method("get" + capitalize(field.getJavaName()), ACC_PUBLIC, sig(type), new CodeBlock() {{
                aload(0);
                getfield(state.currentClass().getClassName(), field.getJavaName(), ci(type));
                areturn();
            }});
        });
    }

    private void generateHashCode(BytecodeGenerator state) {
        state.method("hashCode", ACC_PUBLIC, sig(int.class), new CodeBlock() {{
            if (fields.size() == 1) {
                aload(0);
                DataFieldDefinition field = fields.values().iterator().next();
                getfield(state.currentClass().getClassName(), field.getJavaName(), ci(field.getJavaType()));
                invokeinterface(p(Callable.class), "call", sig(Object.class));
                invokestatic(p(Objects.class), "hashCode", sig(int.class, Object.class));
            } else {
                ldc(fields.size());
                anewarray(p(Object.class));
                AtomicInteger counter = new AtomicInteger();
                fields.values().forEach(field -> {
                    dup();
                    ldc(counter.getAndIncrement());
                    aload(0);
                    getfield(state.currentClass().getClassName(), field.getJavaName(), ci(field.getJavaType()));
                    invokeinterface(p(Callable.class), "call", sig(Object.class));
                    aastore();
                });
                invokestatic(p(Objects.class), "hash", sig(int.class, Object[].class));
            }
            ireturn();
        }});
    }

    private Class<?>[] getParameters() {
        List<Class<?>> parameters = fields.values().stream()
            .map(DataFieldDefinition::getJavaType)
            .collect(toList());
        return parameters.toArray(new Class<?>[parameters.size()]);
    }

    private DataConstructorDefinition withFields(List<DataFieldDefinition> fields) {
        return new DataConstructorDefinition(sourceRange, dataType, symbol, fields);
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

        public Builder withFields(List<DataFieldDefinition> fields) {
            fields.forEach(this::addField);
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
