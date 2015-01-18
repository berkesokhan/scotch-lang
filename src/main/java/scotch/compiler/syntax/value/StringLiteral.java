package scotch.compiler.syntax.value;

import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static scotch.compiler.symbol.Type.sum;
import static scotch.util.StringUtil.quote;
import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.symbol.NameQualifier;
import scotch.compiler.symbol.Type;
import scotch.compiler.syntax.BytecodeGenerator;
import scotch.compiler.syntax.DependencyAccumulator;
import scotch.compiler.syntax.NameAccumulator;
import scotch.compiler.syntax.OperatorDefinitionParser;
import scotch.compiler.syntax.PrecedenceParser;
import scotch.compiler.syntax.TypeChecker;
import scotch.compiler.text.SourceRange;
import scotch.runtime.Callable;

public class StringLiteral extends Value {

    private final SourceRange sourceRange;
    private final String      value;

    StringLiteral(SourceRange sourceRange, String value) {
        this.sourceRange = sourceRange;
        this.value = value;
    }

    @Override
    public Value accumulateDependencies(DependencyAccumulator state) {
        return this;
    }

    @Override
    public Value accumulateNames(NameAccumulator state) {
        return this;
    }

    @Override
    public Value bindMethods(TypeChecker state) {
        return this;
    }

    @Override
    public Value bindTypes(TypeChecker state) {
        return this;
    }

    @Override
    public Value checkTypes(TypeChecker state) {
        return this;
    }

    @Override
    public Value defineOperators(OperatorDefinitionParser state) {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof StringLiteral) {
            StringLiteral other = (StringLiteral) o;
            return Objects.equals(sourceRange, other.sourceRange)
                && Objects.equals(value, other.value);
        } else {
            return false;
        }
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        return new CodeBlock() {{
            ldc(value);
            invokestatic(p(Callable.class), "box", sig(Callable.class, Object.class));
        }};
    }

    @Override
    public SourceRange getSourceRange() {
        return sourceRange;
    }

    @Override
    public Type getType() {
        return sum("scotch.data.string.String");
    }

    public String getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public Value parsePrecedence(PrecedenceParser state) {
        return this;
    }

    @Override
    public Value qualifyNames(NameQualifier state) {
        return this;
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + quote(value) + ")";
    }

    @Override
    public Value withType(Type type) {
        return this;
    }
}
