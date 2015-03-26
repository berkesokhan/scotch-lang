package scotch.compiler.syntax.value;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.compiler.syntax.value.Values.unshuffled;
import static scotch.compiler.util.Either.right;
import static scotch.util.StringUtil.stringify;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.intermediate.IntermediateGenerator;
import scotch.compiler.intermediate.IntermediateValue;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.symbol.type.Type;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.text.SourceLocation;
import scotch.compiler.util.Either;

public class UnshuffledValue extends Value {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceLocation sourceLocation;
    private final List<Value>    values;

    UnshuffledValue(SourceLocation sourceLocation, List<Value> values) {
        this.sourceLocation = sourceLocation;
        this.values = ImmutableList.copyOf(values);
    }

    @Override
    public Value accumulateDependencies(DependencyAccumulator state) {
        return withValues(values.stream()
            .map(value -> value.accumulateDependencies(state))
            .collect(toList()));
    }

    @Override
    public Value accumulateNames(NameAccumulator state) {
        return withValues(values.stream()
            .map(value -> value.accumulateNames(state))
            .collect(toList()));
    }

    @Override
    public IntermediateValue generateIntermediateCode(IntermediateGenerator state) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public Value bindMethods(TypeChecker state) {
        return withValues(values.stream()
            .map(value -> value.bindMethods(state))
            .collect(toList()));
    }

    @Override
    public Value bindTypes(TypeChecker state) {
        return withValues(values.stream()
            .map(value -> value.bindTypes(state))
            .collect(toList()));
    }

    @Override
    public Value checkTypes(TypeChecker state) {
        return withValues(values.stream()
            .map(value -> value.checkTypes(state))
            .collect(toList()));
    }

    @Override
    public Value collapse() {
        if (values.size() == 1) {
            return values.get(0);
        } else {
            return this;
        }
    }

    @Override
    public Value defineOperators(OperatorAccumulator state) {
        return withValues(state.defineValueOperators(values));
    }

    @Override
    public Either<Value, List<Value>> destructure() {
        return right(getValues());
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof UnshuffledValue && Objects.equals(values, ((UnshuffledValue) o).values);
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        throw new IllegalStateException("Can't generate bytecode for unshuffled value");
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public Type getType() {
        return Type.NULL;
    }

    public List<Value> getValues() {
        return values;
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }

    @Override
    public Value parsePrecedence(PrecedenceParser state) {
        if (values.size() == 1) {
            return values.get(0).parsePrecedence(state);
        } else {
            return state.shuffle(this);
        }
    }

    @Override
    public Value qualifyNames(ScopedNameQualifier state) {
        return withValues(state.qualifyValueNames(values));
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + values + ")";
    }

    @Override
    public Value unwrap() {
        Value result = collapse();
        if (result != this) {
            return result.unwrap();
        } else {
            return result;
        }
    }

    public UnshuffledValue withSourceLocation(SourceLocation sourceLocation) {
        return new UnshuffledValue(sourceLocation, values);
    }

    @Override
    public Value withType(Type type) {
        throw new UnsupportedOperationException();
    }

    public UnshuffledValue withValues(List<Value> members) {
        return new UnshuffledValue(sourceLocation, members);
    }

    public static class Builder implements SyntaxBuilder<UnshuffledValue> {

        private final List<Value>              members;
        private       Optional<SourceLocation> sourceLocation;

        private Builder() {
            members = new ArrayList<>();
            sourceLocation = Optional.empty();
        }

        @Override
        public UnshuffledValue build() {
            return unshuffled(
                require(sourceLocation, "Source location"),
                members
            );
        }

        public Builder withMember(Value member) {
            members.add(member);
            return this;
        }

        @Override
        public Builder withSourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = Optional.of(sourceLocation);
            return this;
        }
    }
}
