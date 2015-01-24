package scotch.compiler.syntax.value;

import static java.util.stream.Collectors.toList;
import static scotch.util.StringUtil.stringify;

import java.util.List;
import java.util.Objects;
import com.google.common.collect.ImmutableList;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.symbol.Type;
import scotch.compiler.syntax.BytecodeGenerator;
import scotch.compiler.syntax.DependencyAccumulator;
import scotch.compiler.syntax.NameAccumulator;
import scotch.compiler.syntax.NameQualifier;
import scotch.compiler.syntax.OperatorDefinitionParser;
import scotch.compiler.syntax.PrecedenceParser;
import scotch.compiler.syntax.TypeChecker;
import scotch.compiler.text.SourceRange;

public class Initializer extends Value {

    private final SourceRange            sourceRange;
    private final Value                  value;
    private final List<InitializerField> fields;
    private final Type                   type;

    public Initializer(SourceRange sourceRange, Value value, List<InitializerField> fields, Type type) {
        this.sourceRange = sourceRange;
        this.value = value;
        this.fields = ImmutableList.copyOf(fields);
        this.type = type;
    }

    @Override
    public Value accumulateDependencies(DependencyAccumulator state) {
        return withValue(value.accumulateDependencies(state))
            .withFields(fields.stream()
                .map(field -> field.accumulateDependencies(state))
                .collect(toList()));
    }

    private Value withFields(List<InitializerField> fields) {
        return new Initializer(sourceRange, value, fields, type);
    }

    @Override
    public Value accumulateNames(NameAccumulator state) {
        return withValue(value.accumulateNames(state))
            .withFields(fields.stream()
                .map(field -> field.accumulateNames(state))
                .collect(toList()));
    }

    @Override
    public Value bindMethods(TypeChecker state) {
        return withValue(value.bindMethods(state))
            .withFields(fields.stream()
                .map(field -> field.bindMethods(state))
                .collect(toList()));
    }

    @Override
    public Value bindTypes(TypeChecker state) {
        return withValue(value.bindTypes(state))
            .withFields(fields.stream()
                .map(field -> field.bindTypes(state))
                .collect(toList()));
    }

    @Override
    public Value checkTypes(TypeChecker state) {
        return withValue(value.checkTypes(state))
            .withFields(fields.stream()
                .map(field -> field.checkTypes(state))
                .collect(toList()));
    }

    @Override
    public Value defineOperators(OperatorDefinitionParser state) {
        return withValue(value.defineOperators(state))
            .withFields(fields.stream()
                .map(field -> field.defineOperators(state))
                .collect(toList()));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof Initializer) {
            Initializer other = (Initializer) o;
            return Objects.equals(sourceRange, other.sourceRange)
                && Objects.equals(fields, other.fields);
        } else {
            return false;
        }
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public SourceRange getSourceRange() {
        return sourceRange;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, fields, type);
    }

    @Override
    public Value parsePrecedence(PrecedenceParser state) {
        return withValue(value.parsePrecedence(state))
            .withFields(fields.stream()
                .map(field -> field.parsePrecedence(state))
                .collect(toList()));
    }

    @Override
    public Value qualifyNames(NameQualifier state) {
        return withValue(value.qualifyNames(state))
            .withFields(fields.stream()
                .map(field -> field.qualifyNames(state))
                .collect(toList()));
    }

    private Initializer withValue(Value value) {
        return new Initializer(sourceRange, value, fields, type);
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + value + " <- " + fields.stream().map(InitializerField::getName).collect(toList()) + ")";
    }

    @Override
    public Value withType(Type type) {
        return new Initializer(sourceRange, value, fields, type);
    }
}
