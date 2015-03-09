package scotch.compiler.syntax.pattern;

import java.util.Optional;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.text.SourceRange;

public final class Patterns {

    public static CaptureMatch capture(SourceRange sourceRange, Optional<String> argument, Symbol symbol, Type type) {
        return new CaptureMatch(sourceRange, argument, symbol, type);
    }

    public static EqualMatch equal(SourceRange sourceRange, Optional<String> argument, Value value) {
        return new EqualMatch(sourceRange, argument, value);
    }

    public static IgnorePattern ignore(SourceRange sourceRange, Type type) {
        return new IgnorePattern(sourceRange, type);
    }

    private Patterns() {
        // intentionally empty
    }
}
