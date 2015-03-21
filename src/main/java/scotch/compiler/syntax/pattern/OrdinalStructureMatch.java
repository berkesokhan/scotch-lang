package scotch.compiler.syntax.pattern;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.builder.BuilderUtil.require;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import com.google.common.collect.ImmutableList;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.text.SourceLocation;
import scotch.symbol.Symbol;
import scotch.symbol.descriptor.DataConstructorDescriptor;
import scotch.symbol.type.Type;

@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "sourceLocation")
public class OrdinalStructureMatch extends PatternMatch {

    public static Builder builder() {
        return new Builder();
    }

    @Getter
    private final SourceLocation     sourceLocation;
    private final Optional<String>   argument;
    private final Symbol             constructor;
    @Getter
    private final Type               type;
    private final List<OrdinalField> fields;

    OrdinalStructureMatch(SourceLocation sourceLocation, Optional<String> argument, Symbol constructor, Type type, List<OrdinalField> fields) {
        this.sourceLocation = sourceLocation;
        this.argument = argument;
        this.constructor = constructor;
        this.type = type;
        this.fields = ImmutableList.copyOf(fields);
    }

    @Override
    public PatternMatch accumulateDependencies(DependencyAccumulator state) {
        return this;
    }

    @Override
    public PatternMatch accumulateNames(NameAccumulator state) {
        return map((field, ordinal) -> field.accumulateNames(state));
    }

    @Override
    public PatternMatch bind(String argument, Scope scope) {
        return withArgument(argument).map((field, ordinal) -> field.bind(argument, ordinal, scope));
    }

    @Override
    public PatternMatch bindMethods(TypeChecker state) {
        return map((field, ordinal) -> field.bindMethods(state));
    }

    @Override
    public PatternMatch bindTypes(TypeChecker state) {
        return withType(state.generate(type)).map((field, ordinal) -> field.bindTypes(state));
    }

    @Override
    public PatternMatch checkTypes(TypeChecker state) {
        Optional<DataConstructorDescriptor> dataConstructor = state.scope().getDataConstructor(constructor);
        List<OrdinalField> checkedFields = fields.stream().map(field -> field.checkTypes(state)).collect(toList());
        return this;
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public PatternMatch qualifyNames(ScopedNameQualifier state) {
        return this;
    }

    @Override
    public OrdinalStructureMatch withType(Type type) {
        return new OrdinalStructureMatch(sourceLocation, argument, constructor, type, fields);
    }

    private PatternMatch map(BiFunction<OrdinalField, Integer, OrdinalField> mapper) {
        AtomicInteger counter = new AtomicInteger();
        return new OrdinalStructureMatch(
            sourceLocation, argument, constructor, type,
            fields.stream()
                .map(field -> mapper.apply(field, counter.getAndIncrement()))
                .collect(toList())
        );
    }

    private OrdinalStructureMatch withArgument(String argument) {
        return new OrdinalStructureMatch(sourceLocation, Optional.of(argument), constructor, type, fields);
    }

    public static class Builder implements SyntaxBuilder<OrdinalStructureMatch> {

        private Optional<SourceLocation> sourceLocation = Optional.empty();
        private List<OrdinalField>       fields         = new ArrayList<>();
        private Optional<Symbol>         constructor    = Optional.empty();
        private Optional<Type>           type           = Optional.empty();

        @Override
        public OrdinalStructureMatch build() {
            return new OrdinalStructureMatch(
                require(sourceLocation, "Source location"),
                Optional.empty(),
                require(constructor, "Constructor"),
                require(type, "Type"),
                fields
            );
        }

        public Builder withConstructor(Symbol constructor) {
            this.constructor = Optional.of(constructor);
            return this;
        }

        public Builder withField(OrdinalField field) {
            fields.add(field);
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
