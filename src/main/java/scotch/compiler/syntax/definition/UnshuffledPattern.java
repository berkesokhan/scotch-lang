package scotch.compiler.syntax.definition;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.reference.DefinitionReference.scopeRef;
import static scotch.compiler.syntax.value.PatternMatcher.pattern;
import static scotch.util.StringUtil.stringify;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.syntax.BytecodeGenerator;
import scotch.compiler.syntax.DependencyAccumulator;
import scotch.compiler.syntax.NameAccumulator;
import scotch.compiler.syntax.NameQualifier;
import scotch.compiler.syntax.OperatorDefinitionParser;
import scotch.compiler.syntax.PrecedenceParser;
import scotch.compiler.syntax.TypeChecker;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.value.PatternMatch;
import scotch.compiler.syntax.value.PatternMatcher;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.text.SourceRange;

public class UnshuffledPattern extends Definition {

    private final SourceRange        sourceRange;
    private final Symbol             symbol;
    private final List<PatternMatch> matches;
    private final Value              body;

    UnshuffledPattern(SourceRange sourceRange, Symbol symbol, List<PatternMatch> matches, Value body) {
        this.sourceRange = sourceRange;
        this.symbol = symbol;
        this.matches = ImmutableList.copyOf(matches);
        this.body = body;
    }

    @Override
    public Definition accumulateDependencies(DependencyAccumulator state) {
        return state.scoped(this, () -> withMatches(matches.stream().map(match -> match.accumulateDependencies(state)).collect(toList()))
            .withBody(body.accumulateDependencies(state)));
    }

    @Override
    public Definition accumulateNames(NameAccumulator state) {
        return state.scoped(this, () -> withMatches(matches.stream().map(match -> match.accumulateNames(state)).collect(toList()))
            .withBody(body.accumulateNames(state)));
    }

    public PatternMatcher asPatternMatcher(List<PatternMatch> matches) {
        return pattern(sourceRange, symbol, matches, body);
    }

    @Override
    public Definition bindTypes(TypeChecker state) {
        return withMatches(matches.stream().map(match -> match.bindTypes(state)).collect(toList()))
            .withBody(body.bindTypes(state));
    }

    @Override
    public Definition checkTypes(TypeChecker state) {
        return withMatches(matches.stream().map(match -> match.checkTypes(state)).collect(toList()))
            .withBody(body.checkTypes(state));
    }

    @Override
    public Definition defineOperators(OperatorDefinitionParser state) {
        return state.scoped(this, () -> withBody(body.defineOperators(state)));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof UnshuffledPattern) {
            UnshuffledPattern other = (UnshuffledPattern) o;
            return Objects.equals(symbol, other.symbol)
                && Objects.equals(matches, other.matches)
                && Objects.equals(body, other.body);
        } else {
            return false;
        }
    }

    @Override
    public void generateBytecode(BytecodeGenerator state) {
        // intentionally empty
    }

    public Value getBody() {
        return body;
    }

    public List<PatternMatch> getMatches() {
        return matches;
    }

    @Override
    public DefinitionReference getReference() {
        return scopeRef(symbol);
    }

    public SourceRange getSourceRange() {
        return sourceRange;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, body, matches);
    }

    @Override
    public Optional<Definition> parsePrecedence(PrecedenceParser state) {
        return state.scopedOptional(this, () -> state.shuffle(this));
    }

    @Override
    public Definition qualifyNames(NameQualifier state) {
        return state.scoped(this, () -> withMatches(matches.stream().map(match -> match.qualifyNames(state)).collect(toList()))
            .withBody(body.qualifyNames(state)));
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + symbol + ")";
    }

    public UnshuffledPattern withBody(Value body) {
        return new UnshuffledPattern(sourceRange, symbol, matches, body);
    }

    public UnshuffledPattern withMatches(List<PatternMatch> matches) {
        return new UnshuffledPattern(sourceRange, symbol, matches, body);
    }
}
