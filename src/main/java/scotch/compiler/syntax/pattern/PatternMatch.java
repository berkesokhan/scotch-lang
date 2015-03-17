package scotch.compiler.syntax.pattern;

import static scotch.compiler.util.Either.left;

import java.util.Optional;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.symbol.Operator;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.syntax.value.InstanceMap;
import scotch.compiler.text.SourceRange;
import scotch.compiler.util.Either;
import scotch.compiler.util.Pair;

public abstract class PatternMatch {

    PatternMatch() {
        // intentionally empty
    }

    public abstract PatternMatch accumulateDependencies(DependencyAccumulator state);

    public abstract PatternMatch accumulateNames(NameAccumulator state);

    public Either<PatternMatch, CaptureMatch> asCapture() {
        return left(this);
    }

    public Optional<Pair<CaptureMatch, Operator>> asOperator(Scope scope) {
        return Optional.empty();
    }

    public abstract PatternMatch bind(String argument, Scope scope);

    public abstract PatternMatch bindMethods(TypeChecker state);

    public abstract PatternMatch bindTypes(TypeChecker state);

    public abstract PatternMatch checkTypes(TypeChecker state);

    @Override
    public abstract boolean equals(Object o);

    public abstract CodeBlock generateBytecode(BytecodeGenerator state);

    public abstract SourceRange getSourceRange();

    public abstract Type getType();

    @Override
    public abstract int hashCode();

    public boolean isOperator(Scope scope) {
        return false;
    }

    public String prettyPrint() {
        return "[" + getClass().getSimpleName() + "]";
    }

    public abstract PatternMatch qualifyNames(ScopedNameQualifier state);

    @Override
    public abstract String toString();

    public abstract PatternMatch withType(Type generate);
}
