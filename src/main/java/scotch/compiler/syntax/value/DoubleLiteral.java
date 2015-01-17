package scotch.compiler.syntax.value;

import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static scotch.compiler.symbol.Type.sum;
import static scotch.util.StringUtil.quote;
import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.symbol.Type;
import scotch.compiler.syntax.BytecodeGenerator;
import scotch.compiler.syntax.SyntaxTreeParser;
import scotch.compiler.syntax.TypeChecker;
import scotch.compiler.text.SourceRange;
import scotch.runtime.Callable;

public class DoubleLiteral extends Value {

    private final SourceRange sourceRange;
    private final double      value;

    DoubleLiteral(SourceRange sourceRange, double value) {
        this.sourceRange = sourceRange;
        this.value = value;
    }

    @Override
    public <T> T accept(ValueVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public Value accumulateDependencies(SyntaxTreeParser state) {
        return this;
    }

    @Override
    public Value accumulateNames(SyntaxTreeParser state) {
        return this;
    }

    @Override
    public Value bindMethods(TypeChecker state) {
        return this;
    }

    @Override
    public Value checkTypes(TypeChecker state) {
        return this;
    }

    @Override
    public Value bindTypes(TypeChecker state) {
        return this;
    }

    @Override
    public Value defineOperators(SyntaxTreeParser state) {
        return this;
    }

    @Override
    public Value parsePrecedence(SyntaxTreeParser state) {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof DoubleLiteral) {
            DoubleLiteral other = (DoubleLiteral) o;
            return Objects.equals(sourceRange, other.sourceRange)
                && value == other.value;
        } else {
            return false;
        }
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        return new CodeBlock() {{
            ldc(value);
            invokestatic(p(Callable.class), "box", sig(Callable.class, double.class));
        }};
    }

    @Override
    public SourceRange getSourceRange() {
        return sourceRange;
    }

    @Override
    public Type getType() {
        return sum("scotch.data.double.Double");
    }

    public double getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public Value qualifyNames(SyntaxTreeParser state) {
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
