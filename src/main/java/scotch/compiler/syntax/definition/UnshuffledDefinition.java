package scotch.compiler.syntax.definition;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.compiler.syntax.pattern.Patterns.pattern;
import static scotch.compiler.syntax.reference.DefinitionReference.scopeRef;

import java.util.List;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import scotch.compiler.intermediate.IntermediateGenerator;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.symbol.Symbol;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.pattern.PatternMatch;
import scotch.compiler.syntax.pattern.PatternCase;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.text.SourceLocation;

@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "sourceLocation")
public class UnshuffledDefinition extends Definition {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceLocation     sourceLocation;
    private final Symbol             symbol;
    private final List<PatternMatch> matches;
    private final Value              body;

    UnshuffledDefinition(SourceLocation sourceLocation, Symbol symbol, List<PatternMatch> matches, Value body) {
        this.sourceLocation = sourceLocation;
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

    public PatternCase asPatternMatcher(List<PatternMatch> matches) {
        return pattern(sourceLocation, symbol, matches, body);
    }

    @Override
    public Definition checkTypes(TypeChecker state) {
        return withMatches(matches.stream().map(match -> match.checkTypes(state)).collect(toList()))
            .withBody(body.checkTypes(state));
    }

    @Override
    public Definition defineOperators(OperatorAccumulator state) {
        return state.scoped(this, () -> withBody(body.defineOperators(state)));
    }

    @Override
    public void generateBytecode(BytecodeGenerator state) {
        throw new IllegalStateException("Can't generate bytecode from unshuffled definition");
    }

    @Override
    public void generateIntermediateCode(IntermediateGenerator state) {
        throw new UnsupportedOperationException(); // TODO
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

    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public Optional<Definition> parsePrecedence(PrecedenceParser state) {
        return state.scopedOptional(this, () -> state.shuffle(this));
    }

    @Override
    public Definition qualifyNames(ScopedNameQualifier state) {
        return state.scoped(this, () -> withMatches(matches.stream().map(match -> match.qualifyNames(state)).collect(toList()))
            .withBody(body.qualifyNames(state)));
    }

    public UnshuffledDefinition withBody(Value body) {
        return new UnshuffledDefinition(sourceLocation, symbol, matches, body);
    }

    public UnshuffledDefinition withMatches(List<PatternMatch> matches) {
        return new UnshuffledDefinition(sourceLocation, symbol, matches, body);
    }

    public static class Builder implements SyntaxBuilder<UnshuffledDefinition> {

        private Optional<Symbol>             symbol;
        private Optional<List<PatternMatch>> matches;
        private Optional<Value>              body;
        private Optional<SourceLocation>     sourceLocation;

        private Builder() {
            symbol = Optional.empty();
            matches = Optional.empty();
            body = Optional.empty();
            sourceLocation = Optional.empty();
        }

        @Override
        public UnshuffledDefinition build() {
            return Definitions.unshuffled(
                require(sourceLocation, "Source location"),
                require(symbol, "Unshuffled pattern symbol"),
                require(matches, "Unshuffled pattern matches"),
                require(body, "Unshuffled pattern body")
            );
        }

        public Builder withBody(Value body) {
            this.body = Optional.of(body);
            return this;
        }

        public Builder withMatches(List<PatternMatch> matches) {
            this.matches = Optional.of(matches);
            return this;
        }

        @Override
        public Builder withSourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = Optional.of(sourceLocation);
            return this;
        }

        public Builder withSymbol(Symbol symbol) {
            this.symbol = Optional.of(symbol);
            return this;
        }
    }
}
