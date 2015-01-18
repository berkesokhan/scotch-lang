package scotch.compiler.syntax.value;

import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import java.util.Optional;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.symbol.Type;
import scotch.compiler.syntax.BytecodeGenerator;
import scotch.compiler.syntax.DependencyAccumulator;
import scotch.compiler.syntax.NameAccumulator;
import scotch.compiler.syntax.NameQualifier;
import scotch.compiler.syntax.TypeChecker;
import scotch.compiler.text.SourceRange;

public class EqualMatch extends PatternMatch {

    private final SourceRange      sourceRange;
    private final Optional<String> argument;
    private final Value            value;

    EqualMatch(SourceRange sourceRange, Optional<String> argument, Value value) {
        this.sourceRange = sourceRange;
        this.argument = argument;
        this.value = value;
    }

    @Override
    public PatternMatch accumulateDependencies(DependencyAccumulator state) {
        return withValue(value.accumulateDependencies(state));
    }

    @Override
    public PatternMatch accumulateNames(NameAccumulator state) {
        return this;
    }

    @Override
    public PatternMatch checkTypes(TypeChecker state) {
        return withValue(value.checkTypes(state));
    }

    @Override
    public PatternMatch bind(String argument) {
        if (this.argument.isPresent()) {
            throw new IllegalStateException();
        } else {
            return new EqualMatch(sourceRange, Optional.of(argument), value);
        }
    }

    @Override
    public PatternMatch bindTypes(TypeChecker state) {
        return withValue(value.bindTypes(state));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof EqualMatch) {
            EqualMatch other = (EqualMatch) o;
            return Objects.equals(sourceRange, other.sourceRange)
                && Objects.equals(argument, other.argument)
                && Objects.equals(value, other.value);
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
        return value.getType();
    }

    public Value getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(argument, value);
    }

    @Override
    public PatternMatch qualifyNames(NameQualifier state) {
        return withValue(value.qualifyNames(state));
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + value + ")";
    }

    @Override
    public EqualMatch withType(Type generate) {
        return new EqualMatch(sourceRange, argument, value);
    }

    public EqualMatch withSourceRange(SourceRange sourceRange) {
        return new EqualMatch(sourceRange, argument, value);
    }

    public EqualMatch withValue(Value value) {
        return new EqualMatch(sourceRange, argument, value);
    }
}
