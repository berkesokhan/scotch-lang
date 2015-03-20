package scotch.compiler.syntax.pattern;

import java.util.List;
import java.util.Optional;
import scotch.symbol.Symbol;
import scotch.symbol.type.Type;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.text.SourceLocation;

public final class Patterns {

    public static CaptureMatch capture(SourceLocation sourceLocation, Optional<String> argument, Symbol symbol, Type type) {
        return new CaptureMatch(sourceLocation, argument, symbol, type);
    }

    public static EqualMatch equal(SourceLocation sourceLocation, Optional<String> argument, Value value) {
        return new EqualMatch(sourceLocation, argument, value);
    }

    public static IgnorePattern ignore(SourceLocation sourceLocation, Type type) {
        return new IgnorePattern(sourceLocation, type);
    }

    public static PatternCase pattern(SourceLocation sourceLocation, Symbol symbol, List<PatternMatch> matches, Value body) {
        return new PatternCase(sourceLocation, symbol, matches, body);
    }

    private Patterns() {
        // intentionally empty
    }
}
