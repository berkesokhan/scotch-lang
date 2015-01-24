package scotch.compiler.syntax.definition;

import static scotch.compiler.syntax.builder.BuilderUtil.require;

import java.util.Objects;
import java.util.Optional;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.syntax.value.Argument;
import scotch.compiler.syntax.value.Identifier;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.text.SourceRange;

public class DataFieldDefinition {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceRange sourceRange;
    private final String      name;
    private final Type        type;

    public DataFieldDefinition(SourceRange sourceRange, String name, Type type) {
        this.sourceRange = sourceRange;
        this.name = name;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof DataFieldDefinition) {
            DataFieldDefinition other = (DataFieldDefinition) o;
            return Objects.equals(name, other.name)
                && Objects.equals(type, other.type);
        } else {
            return false;
        }
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    public Argument toArgument(Scope scope) {
        return Argument.builder()
            .withName(name)
            .withSourceRange(sourceRange)
            .withType(scope.reserveType())
            .build();
    }

    @Override
    public String toString() {
        return name + " " + type;
    }

    public Value toValue(Scope scope) {
        return Identifier.builder()
            .withSymbol(Symbol.fromString(name))
            .withType(scope.reserveType())
            .withSourceRange(sourceRange)
            .build();
    }

    public static final class Builder implements SyntaxBuilder<DataFieldDefinition> {

        private Optional<SourceRange> sourceRange;
        private Optional<String> name;
        private Optional<Type>   type;

        private Builder() {
            name = Optional.empty();
            type = Optional.empty();
        }

        public DataFieldDefinition build() {
            return new DataFieldDefinition(
                require(sourceRange, "Source range"),
                require(name, "Field name"),
                require(type, "Field type")
            );
        }

        public Builder withName(String name) {
            this.name = Optional.of(name);
            return this;
        }

        @Override
        public SyntaxBuilder<DataFieldDefinition> withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }

        public Builder withType(Type type) {
            this.type = Optional.of(type);
            return this;
        }
    }
}
