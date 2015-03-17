package scotch.compiler.syntax.pattern;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.compiler.syntax.definition.Definitions.scopeDef;
import static scotch.compiler.syntax.reference.DefinitionReference.scopeRef;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import com.google.common.collect.ImmutableList;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.syntax.Scoped;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.definition.Definition;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.text.SourceRange;

@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "sourceRange")
public class PatternCase implements Scoped {

    public static Builder builder() {
        return new Builder();
    }

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

    public PatternCase defineOperators(OperatorAccumulator state) {
        return state.scoped(this, () -> withBody(body.defineOperators(state)));
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

    public PatternCase withBody(Value body) {
        return new PatternCase(sourceRange, symbol, matches, body);
    }

    public PatternCase withMatches(List<PatternMatch> matches) {
        return new PatternCase(sourceRange, symbol, matches, body);
    }

    public PatternCase withType(Type type) {
        return new PatternCase(sourceRange, symbol, matches, body.withType(type));
    }

    private PatternCase withSymbol(Symbol symbol) {
        return new PatternCase(sourceRange, symbol, matches, body);
    }

    public static class Builder implements SyntaxBuilder<PatternCase> {

        private Optional<SourceRange> sourceRange = Optional.empty();
        private Optional<Symbol> symbol = Optional.empty();
        private Optional<List<PatternMatch>> matches = Optional.empty();
        private Optional<Value> body = Optional.empty();

        @Override
        public PatternCase build() {
            return new PatternCase(
                require(sourceRange, "Source range"),
                require(symbol, "Symbol"),
                require(matches, "Pattern matches"),
                require(body, "Pattern body")
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
        public Builder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }

        public Builder withSymbol(Symbol symbol) {
            this.symbol = Optional.of(symbol);
            return this;
        }
    }
}
