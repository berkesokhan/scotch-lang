package scotch.compiler.syntax.pattern;

import java.util.List;
import java.util.Optional;
import scotch.symbol.Symbol;
import scotch.symbol.type.Type;
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

    public static PatternCase pattern(SourceRange sourceRange, Symbol symbol, List<PatternMatch> matches, Value body) {
        return new PatternCase(sourceRange, symbol, matches, body);
    }

    private Patterns() {
        // intentionally empty
    }
}
