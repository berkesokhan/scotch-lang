package scotch.compiler.syntax.value;

import static scotch.data.either.Either.left;

import java.util.Optional;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.symbol.NameQualifier;
import scotch.compiler.symbol.Operator;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.syntax.BytecodeGenerator;
import scotch.compiler.syntax.DependencyAccumulator;
import scotch.compiler.syntax.NameAccumulator;
import scotch.compiler.syntax.TypeChecker;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.text.SourceRange;
import scotch.data.either.Either;
import scotch.data.tuple.Tuple2;

public abstract class PatternMatch {

    public static CaptureMatch capture(SourceRange sourceRange, Optional<String> argument, Symbol symbol, Type type) {
        return new CaptureMatch(sourceRange, argument, symbol, type);
    }

    public static EqualMatch equal(SourceRange sourceRange, Optional<String> argument, Value value) {
        return new EqualMatch(sourceRange, argument, value);
    }

    PatternMatch() {
        // intentionally empty
    }

    public abstract PatternMatch accumulateDependencies(DependencyAccumulator state);

    public abstract PatternMatch accumulateNames(NameAccumulator state);

    public Either<PatternMatch, CaptureMatch> asCapture() {
        return left(this);
    }

    public Optional<Tuple2<CaptureMatch, Operator>> asOperator(Scope scope) {
        return Optional.empty();
    }

    public abstract PatternMatch bind(String argument);

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

    public abstract PatternMatch qualifyNames(NameQualifier state);

    @Override
    public abstract String toString();

    public abstract PatternMatch withType(Type generate);
}
