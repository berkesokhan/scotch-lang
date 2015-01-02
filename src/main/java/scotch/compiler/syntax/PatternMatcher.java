package scotch.compiler.syntax;

import static scotch.compiler.syntax.DefinitionReference.scopeRef;
import static scotch.util.StringUtil.stringify;

import java.util.List;
import java.util.Objects;
import com.google.common.collect.ImmutableList;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.text.SourceRange;

public class PatternMatcher {

    public static PatternMatcher pattern(SourceRange sourceRange, Symbol symbol, List<PatternMatch> matches, Value body) {
        return new PatternMatcher(sourceRange, symbol, matches, body);
    }

    private final SourceRange        sourceRange;
    private final Symbol             symbol;
    private final List<PatternMatch> matches;
    private final Value              body;

    private PatternMatcher(SourceRange sourceRange, Symbol symbol, List<PatternMatch> matches, Value body) {
        this.sourceRange = sourceRange;
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

    public int getArity() {
        return matches.size();
    }

    public Value getBody() {
        return body;
    }

    public List<PatternMatch> getMatches() {
        return matches;
    }

    public DefinitionReference getReference() {
        return scopeRef(symbol);
    }

    public SourceRange getSourceRange() {
        return sourceRange;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public Type getType() {
        return body.getType();
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
        return new PatternMatcher(sourceRange, symbol, matches, body);
    }

    public PatternMatcher withMatches(List<PatternMatch> matches) {
        return new PatternMatcher(sourceRange, symbol, matches, body);
    }

    public PatternMatcher withType(Type type) {
        return new PatternMatcher(sourceRange, symbol, matches, body.withType(type));
    }
}
