package scotch.compiler.syntax.value;

import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.syntax.BytecodeGenerator;
import scotch.compiler.syntax.DependencyAccumulator;
import scotch.compiler.syntax.NameAccumulator;
import scotch.compiler.syntax.NameQualifier;
import scotch.compiler.syntax.OperatorDefinitionParser;
import scotch.compiler.syntax.PrecedenceParser;
import scotch.compiler.syntax.TypeChecker;
import scotch.compiler.text.SourceRange;

public class InitializerField {

    public static InitializerField field(SourceRange sourceRange, String name, Value value) {
        return new InitializerField(sourceRange, name, value);
    }

    private final SourceRange sourceRange;
    private final String      name;
    private final Value       value;

    InitializerField(SourceRange sourceRange, String name, Value value) {
        this.sourceRange = sourceRange;
        this.name = name;
        this.value = value;
    }

    public InitializerField accumulateDependencies(DependencyAccumulator state) {
        return withValue(value.accumulateDependencies(state));
    }

    public InitializerField accumulateNames(NameAccumulator state) {
        return withValue(value.accumulateNames(state));
    }

    public InitializerField bindMethods(TypeChecker state) {
        return withValue(value.bindMethods(state));
    }

    public InitializerField bindTypes(TypeChecker state) {
        return withValue(value.bindTypes(state));
    }

    public InitializerField checkTypes(TypeChecker state) {
        return withValue(value.checkTypes(state));
    }

    public InitializerField defineOperators(OperatorDefinitionParser state) {
        return withValue(value.defineOperators(state));
    }

    public CodeBlock generateBytecode(BytecodeGenerator state) {
        return new CodeBlock() {{
            value.generateBytecode(state);
        }};
    }

    public InitializerField parsePrecedence(PrecedenceParser state) {
        return withValue(value.parsePrecedence(state));
    }

    public InitializerField qualifyNames(NameQualifier state) {
        return withValue(value.qualifyNames(state));
    }

    private InitializerField withValue(Value value) {
        return new InitializerField(sourceRange, name, value);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof InitializerField) {
            InitializerField other = (InitializerField) o;
            return Objects.equals(sourceRange, other.sourceRange)
                && Objects.equals(name, other.name)
                && Objects.equals(value, other.value);
        } else {
            return false;
        }
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + name + ")";
    }
}
