package scotch.compiler.steps;

import java.util.List;
import com.google.common.collect.ImmutableList;
import scotch.symbol.Symbol;
import scotch.compiler.syntax.pattern.PatternMatch;

public class ShuffledPattern {

    private final Symbol             symbol;
    private final List<PatternMatch> matches;

    public ShuffledPattern(Symbol symbol, List<PatternMatch> matches) {
        this.symbol = symbol;
        this.matches = ImmutableList.copyOf(matches);
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public List<PatternMatch> getMatches() {
        return matches;
    }
}
