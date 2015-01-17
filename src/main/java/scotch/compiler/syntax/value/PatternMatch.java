package scotch.compiler.syntax.value;

import java.util.Optional;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.syntax.BytecodeGenerator;
import scotch.compiler.syntax.SyntaxTreeParser;
import scotch.compiler.syntax.TypeChecker;
import scotch.compiler.text.SourceRange;

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

    public abstract <T> T accept(PatternMatchVisitor<T> visitor);

    public abstract PatternMatch accumulateDependencies(SyntaxTreeParser state);

    public abstract PatternMatch accumulateNames(SyntaxTreeParser state);

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

    public abstract PatternMatch qualifyNames(SyntaxTreeParser state);

    @Override
    public abstract String toString();

    public abstract PatternMatch withType(Type generate);

    public interface PatternMatchVisitor<T> {

        default T visit(CaptureMatch match) {
            return visitOtherwise(match);
        }

        default T visit(EqualMatch match) {
            return visitOtherwise(match);
        }

        default T visitOtherwise(PatternMatch match) {
            throw new UnsupportedOperationException("Can't visit " + match.getClass().getSimpleName());
        }
    }
}
