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

    public static OrdinalField ordinalField(SourceLocation sourceLocation, Optional<String> argument, Optional<String> field, Type type, PatternMatch patternMatch) {
        return new OrdinalField(sourceLocation, argument, field, type, patternMatch);
    }

    public static PatternCase pattern(SourceLocation sourceLocation, Symbol symbol, List<PatternMatch> patternMatches, Value body) {
        return new PatternCase(sourceLocation, symbol, patternMatches, body);
    }

    public static OrdinalStructureMatch structure(SourceLocation sourceLocation, Optional<String> argument, Symbol dataType, Type type, List<OrdinalField> fields) {
        return new OrdinalStructureMatch(sourceLocation, argument, dataType, type, fields);
    }

    public static UnshuffledStructureMatch unshuffledMatch(SourceLocation sourceLocation, Type type, List<PatternMatch> patternMatches) {
        return new UnshuffledStructureMatch(sourceLocation, type, patternMatches);
    }

    private Patterns() {
        // intentionally empty
    }
}
