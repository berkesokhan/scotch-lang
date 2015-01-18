package scotch.compiler.syntax.value;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.reference.DefinitionReference.scopeRef;
import static scotch.compiler.syntax.value.Value.scopeDef;
import static scotch.util.StringUtil.stringify;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.google.common.collect.ImmutableList;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.syntax.DependencyAccumulator;
import scotch.compiler.syntax.NameAccumulator;
import scotch.compiler.syntax.NameQualifier;
import scotch.compiler.syntax.PrecedenceParser;
import scotch.compiler.syntax.Scoped;
import scotch.compiler.syntax.TypeChecker;
import scotch.compiler.syntax.definition.Definition;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.text.SourceRange;

public class PatternMatcher implements Scoped {

    public static PatternMatcher pattern(SourceRange sourceRange, Symbol symbol, List<PatternMatch> matches, Value body) {
        return new PatternMatcher(sourceRange, symbol, matches, body);
    }

    private final SourceRange        sourceRange;
    private final Symbol             symbol;
    private final List<PatternMatch> matches;
    private final Value              body;

    PatternMatcher(SourceRange sourceRange, Symbol symbol, List<PatternMatch> matches, Value body) {
        this.sourceRange = sourceRange;
        this.symbol = symbol;
        this.matches = ImmutableList.copyOf(matches);
        this.body = body;
    }

    public PatternMatcher accumulateDependencies(DependencyAccumulator state) {
        return state.keep(withMatches(matches.stream().map(match -> match.accumulateDependencies(state)).collect(toList()))
            .withBody(body.accumulateDependencies(state)));
    }

    public PatternMatcher accumulateNames(NameAccumulator state) {
        return state.scoped(this, () -> withMatches(matches.stream().map(match -> match.accumulateNames(state)).collect(toList()))
            .withBody(body.accumulateNames(state)));
    }

    public PatternMatcher analyzeTypes(TypeChecker state) {
        return state.scoped(this, () -> {
            List<PatternMatch> matches = this.matches.stream()
                .map(match -> match.checkTypes(state))
                .map(match -> match.withType(state.generate(match.getType())))
                .collect(toList());
            Value body = this.body.checkTypes(state);
            return withMatches(matches)
                .withBody(body.withType(state.generate(body.getType())));
        });
    }

    public PatternMatcher bindMethods(TypeChecker state) {
        return withBody(body.bindMethods(state));
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

    @Override
    public Definition getDefinition() {
        return scopeDef(this);
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

    public PatternMatcher parsePrecedence(PrecedenceParser state) {
        return state.scoped(this, () -> {
            List<PatternMatch> boundMatches = new ArrayList<>();
            int counter = 0;
            for (PatternMatch match : matches) {
                boundMatches.add(match.bind("#" + counter++));
            }
            return withSymbol(state.reserveSymbol())
                .withMatches(boundMatches)
                .withBody(body.parsePrecedence(state).unwrap());
        });
    }

    public PatternMatcher qualifyNames(NameQualifier state) {
        return state.scoped(this, () -> withMatches(matches.stream()
            .map(match -> match.qualifyNames(state))
            .collect(toList()))
        .withBody(body.qualifyNames(state)));
    }

    private PatternMatcher withSymbol(Symbol symbol) {
        return new PatternMatcher(sourceRange, symbol, matches, body);
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
