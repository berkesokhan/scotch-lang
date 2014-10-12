package scotch.compiler.ast;

import static scotch.compiler.util.TextUtil.stringify;

import java.util.List;
import java.util.Objects;
import com.google.common.collect.ImmutableList;

public class PatternMatcher {

    public static PatternMatcher pattern(List<PatternMatch> matches, Value body) {
        return new PatternMatcher(matches, body);
    }

    private final List<PatternMatch> matches;
    private final Value              body;

    private PatternMatcher(List<PatternMatch> matches, Value body) {
        this.matches = ImmutableList.copyOf(matches);
        this.body = body;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof PatternMatcher) {
            PatternMatcher other = (PatternMatcher) o;
            return Objects.equals(matches, other.matches)
                && Objects.equals(body, other.body);
        } else {
            return false;
        }
    }

    public Value getBody() {
        return body;
    }

    @Override
    public int hashCode() {
        return Objects.hash(matches, body);
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + matches + " -> " + body + ")";
    }

    public PatternMatcher withBody(Value body) {
        return new PatternMatcher(matches, body);
    }
}
