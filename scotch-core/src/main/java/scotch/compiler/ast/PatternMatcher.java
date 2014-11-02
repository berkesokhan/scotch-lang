package scotch.compiler.ast;

import static scotch.compiler.ast.DefinitionReference.patternRef;
import static scotch.compiler.util.TextUtil.stringify;

import java.util.List;
import java.util.Objects;
import com.google.common.collect.ImmutableList;

public class PatternMatcher {

    public static PatternMatcher pattern(Symbol symbol, List<PatternMatch> matches, Value body) {
        return new PatternMatcher(symbol, matches, body);
    }

    private final Symbol             symbol;
    private final List<PatternMatch> matches;
    private final Value              body;

    private PatternMatcher(Symbol symbol, List<PatternMatch> matches, Value body) {
        this.symbol = symbol;
        this.matches = ImmutableList.copyOf(matches);
        this.body = body;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof PatternMatcher) {
            PatternMatcher other = (PatternMatcher) o;
            return Objects.equals(symbol, other.symbol)
                && Objects.equals(matches, other.matches)
                && Objects.equals(body, other.body);
        } else {
            return false;
        }
    }

    public Value getBody() {
        return body;
    }

    public List<PatternMatch> getMatches() {
        return matches;
    }

    public DefinitionReference getReference() {
        return patternRef(symbol);
    }

    public Type getType() {
        return matches.stream()
            .map(PatternMatch::getType)
            .reduce(body.getType(), Type::fn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, matches, body);
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + symbol + ")";
    }

    public PatternMatcher withBody(Value body) {
        return new PatternMatcher(symbol, matches, body);
    }

    public PatternMatcher withMatches(List<PatternMatch> matches) {
        return new PatternMatcher(symbol, matches, body);
    }

    public PatternMatcher withType(Type type) {
        return new PatternMatcher(symbol, matches, body);
    }
}
