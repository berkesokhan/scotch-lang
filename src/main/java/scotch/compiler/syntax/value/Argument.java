package scotch.compiler.syntax.value;

import static scotch.compiler.symbol.Symbol.unqualified;
import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.syntax.BytecodeGenerator;
import scotch.compiler.syntax.DependencyAccumulator;
import scotch.compiler.syntax.NameAccumulator;
import scotch.compiler.syntax.NameQualifier;
import scotch.compiler.syntax.OperatorDefinitionParser;
import scotch.compiler.syntax.PrecedenceParser;
import scotch.compiler.syntax.TypeChecker;
import scotch.compiler.text.SourceRange;

public class Argument extends Value {

    private final SourceRange sourceRange;
    private final String      name;
    private final Type        type;

    Argument(SourceRange sourceRange, String name, Type type) {
        this.sourceRange = sourceRange;
        this.name = name;
        this.type = type;
    }

    @Override
    public Value accumulateDependencies(DependencyAccumulator state) {
        return this;
    }

    @Override
    public Value accumulateNames(NameAccumulator state) {
        state.defineValue(getSymbol(), type);
        return this;
    }

    @Override
    public Value bindMethods(TypeChecker state) {
        return this;
    }

    @Override
    public Value bindTypes(TypeChecker state) {
        return withType(state.generate(getType()));
    }

    @Override
    public Value checkTypes(TypeChecker state) {
        state.capture(getSymbol());
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
        } else if (o instanceof Argument) {
            Argument other = (Argument) o;
            return Objects.equals(sourceRange, other.sourceRange)
                && Objects.equals(name, other.name)
                && Objects.equals(type, other.type);
        } else {
            return false;
        }
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        return new CodeBlock() {{
            aload(state.getVariable(name));
        }};
    }

    public String getName() {
        return name;
    }

    @Override
    public SourceRange getSourceRange() {
        return sourceRange;
    }

    public Symbol getSymbol() {
        return unqualified(name);
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
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
        return stringify(this) + "(" + name + " :: " + type + ")";
    }

    @Override
    public Argument withType(Type type) {
        return arg(sourceRange, name, type);
    }
}
