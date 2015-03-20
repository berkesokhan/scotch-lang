package scotch.compiler.syntax.definition;

import static scotch.compiler.util.Either.left;

import java.util.Optional;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.symbol.Symbol;
import scotch.compiler.syntax.Scoped;
import scotch.compiler.text.SourceLocation;
import scotch.compiler.util.Either;

public abstract class Definition implements Scoped {

    protected Definition() {
        // intentionally empty
    }

    public abstract Definition accumulateDependencies(DependencyAccumulator state);

    public abstract Definition accumulateNames(NameAccumulator state);

    public Either<Definition, ValueSignature> asSignature() {
        return left(this);
    }

    public Optional<Symbol> asSymbol() {
        return Optional.empty();
    }

    public Either<Definition, ValueDefinition> asValue() {
        return left(this);
    }

    public abstract Definition checkTypes(TypeChecker state);

    public abstract Definition defineOperators(OperatorAccumulator state);

    @Override
    public abstract boolean equals(Object o);

    public abstract void generateBytecode(BytecodeGenerator state);

    @Override
    public Definition getDefinition() {
        return this;
    }

    public abstract SourceLocation getSourceLocation();

    @Override
    public abstract int hashCode();

    public void markLine(CodeBlock codeBlock) {
        getSourceLocation().markLine(codeBlock);
    }

    public abstract Optional<Definition> parsePrecedence(PrecedenceParser state);

    public abstract Definition qualifyNames(ScopedNameQualifier state);

    @Override
    public abstract String toString();
}
