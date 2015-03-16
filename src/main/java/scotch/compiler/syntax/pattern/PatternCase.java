package scotch.compiler.syntax.pattern;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.definition.Definitions.scopeDef;
import static scotch.compiler.syntax.reference.DefinitionReference.scopeRef;
import static scotch.util.StringUtil.stringify;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import com.google.common.collect.ImmutableList;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.syntax.Scoped;
import scotch.compiler.syntax.definition.Definition;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.text.SourceRange;

public class PatternCase implements Scoped {

    private final SourceRange        sourceRange;
    private final Symbol             symbol;
    private final List<PatternMatch> matches;
    private final Value              body;

    PatternCase(SourceRange sourceRange, Symbol symbol, List<PatternMatch> matches, Value body) {
        this.sourceRange = sourceRange;
        this.symbol = symbol;
        this.matches = ImmutableList.copyOf(matches);
        this.body = body;
    }

    public PatternCase accumulateDependencies(DependencyAccumulator state) {
        return state.keep(withMatches(matches.stream().map(match -> match.accumulateDependencies(state)).collect(toList()))
            .withBody(body.accumulateDependencies(state)));
    }

    public PatternCase accumulateNames(NameAccumulator state) {
        return state.scoped(this, () -> withMatches(matches.stream().map(match -> match.accumulateNames(state)).collect(toList()))
            .withBody(body.accumulateNames(state)));
    }

    public PatternCase checkTypes(TypeChecker state) {
        return state.scoped(this, () -> {
            matches.stream()
                .map(PatternMatch::getType)
                .forEach(state::specialize);
            try {
                return withMatches(matches.stream()
                    .map(match -> match.checkTypes(state))
                    .collect(toList()))
                    .withBody(body.checkTypes(state));
            } finally {
                matches.stream()
                    .map(PatternMatch::getType)
                    .forEach(state::generalize);
            }
        });
    }

    public PatternCase bindMethods(TypeChecker state) {
        return state.scoped(this,
            () -> withMatches(matches.stream()
                .map(match -> match.bindMethods(state))
                .collect(toList()))
            .withBody(body.bindMethods(state)));
    }

    public PatternCase bindTypes(TypeChecker state) {
        return state.scoped(this,
            () -> withMatches(matches.stream()
                .map(match -> match.bindTypes(state))
                .collect(toList()))
            .withBody(body.bindTypes(state)));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof PatternCase) {
            PatternCase other = (PatternCase) o;
            return Objects.equals(symbol, other.symbol)
                && Objects.equals(matches, other.matches)
                && Objects.equals(body, other.body);
        } else {
            return false;
        }
    }

    public CodeBlock generateBytecode(BytecodeGenerator state) {
        return new CodeBlock() {{
            state.generate(PatternCase.this, () -> {
                label(state.beginCase());
                state.beginMatches();
                matches.forEach(match -> append(match.generateBytecode(state)));
                append(body.generateBytecode(state));
                go_to(state.endCase());
                state.endMatches();
            });
        }};
    }

    public int getArity() {
        return matches.size();
    }

    public Value getBody() {
        return body;
    }

    @Override
    public Definition getDefinition() {
        return scopeDef(sourceRange, symbol);
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

    public PatternCase parsePrecedence(PrecedenceParser state) {
        return state.scoped(this, () -> {
            AtomicInteger counter = new AtomicInteger();
            List<PatternMatch> boundMatches = matches.stream()
                .map(match -> match.bind("#" + counter.getAndIncrement(), state.scope()))
                .collect(toList());
            return withSymbol(state.reserveSymbol())
                .withMatches(boundMatches)
                .withBody(body.parsePrecedence(state).unwrap());
        });
    }

    public PatternCase qualifyNames(ScopedNameQualifier state) {
        return state.scoped(this, () -> withMatches(matches.stream()
            .map(match -> match.qualifyNames(state))
            .collect(toList()))
        .withBody(body.qualifyNames(state)));
    }

    private PatternCase withSymbol(Symbol symbol) {
        return new PatternCase(sourceRange, symbol, matches, body);
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + symbol + ")";
    }

    public PatternCase withBody(Value body) {
        return new PatternCase(sourceRange, symbol, matches, body);
    }

    public PatternCase withMatches(List<PatternMatch> matches) {
        return new PatternCase(sourceRange, symbol, matches, body);
    }

    public PatternCase withType(Type type) {
        return new PatternCase(sourceRange, symbol, matches, body.withType(type));
    }
}
