package scotch.compiler.syntax.definition;

import static me.qmx.jitescript.util.CodegenUtils.ci;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static scotch.symbol.Symbol.symbol;
import static scotch.symbol.descriptor.DataFieldDescriptor.field;
import static scotch.compiler.syntax.builder.BuilderUtil.require;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.symbol.NameQualifier;
import scotch.symbol.Symbol;
import scotch.symbol.descriptor.DataFieldDescriptor;
import scotch.symbol.type.FunctionType;
import scotch.symbol.type.Type;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.value.Argument;
import scotch.compiler.syntax.value.Identifier;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.text.SourceLocation;
import scotch.runtime.Applicable;
import scotch.runtime.Callable;

@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class DataFieldDefinition implements Comparable<DataFieldDefinition> {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceLocation sourceLocation;
    private final int            ordinal;
    private final String         name;
    private final Type           type;

    @Override
    public int compareTo(DataFieldDefinition o) {
        return ordinal - o.ordinal;
    }

    public void generateBytecode(BytecodeGenerator state) {
        state.field(Symbol.toJavaName(name), ACC_PRIVATE | ACC_FINAL, ci(getJavaType()));
    }

    public DataFieldDescriptor getDescriptor() {
        return field(ordinal, name, type);
    }

    public String getJavaName() {
        return Symbol.toJavaName(name);
    }

    public Class<?> getJavaType() {
        return type instanceof FunctionType ? Applicable.class : Callable.class;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public DataFieldDefinition qualifyNames(NameQualifier state) {
        return withType(type.qualifyNames(state));
    }

    public Argument toArgument() {
        return Argument.builder()
            .withName(name)
            .withSourceLocation(sourceLocation)
            .withType(type)
            .build();
    }

    @Override
    public String toString() {
        return name + " " + type;
    }

    public Value toValue() {
        return Identifier.builder()
            .withSymbol(symbol(name))
            .withType(type)
            .withSourceLocation(sourceLocation)
            .build();
    }

    private DataFieldDefinition withType(Type type) {
        return new DataFieldDefinition(sourceLocation, ordinal, name, type);
    }

    public static final class Builder implements SyntaxBuilder<DataFieldDefinition> {

        private Optional<SourceLocation> sourceLocation = Optional.empty();
        private Optional<Integer>        ordinal     = Optional.empty();
        private Optional<String>         name        = Optional.empty();
        private Optional<Type>           type        = Optional.empty();

        public DataFieldDefinition build() {
            return new DataFieldDefinition(
                require(sourceLocation, "Source location"),
                require(ordinal, "Ordinal"),
                require(name, "Field name"),
                require(type, "Field type")
            );
        }

        public Builder withName(String name) {
            this.name = Optional.of(name);
            return this;
        }

        public Builder withOrdinal(int ordinal) {
            this.ordinal = Optional.of(ordinal);
            return this;
        }

        @Override
        public Builder withSourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = Optional.of(sourceLocation);
            return this;
        }

        public Builder withType(Type type) {
            this.type = Optional.of(type);
            return this;
        }
    }
}
