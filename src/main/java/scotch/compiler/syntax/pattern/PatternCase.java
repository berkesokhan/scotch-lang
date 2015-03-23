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
import scotch.symbol.Symbol;
import scotch.symbol.type.Type;
import scotch.compiler.syntax.Scoped;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.definition.Definition;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.text.SourceLocation;

@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "sourceLocation")
public class PatternCase implements Scoped {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceLocation     sourceLocation;
    private final Symbol             symbol;
    private final List<PatternMatch> patternMatches;
    private final Value              body;

    PatternCase(SourceLocation sourceLocation, Symbol symbol, List<PatternMatch> patternMatches, Value body) {
        this.sourceLocation = sourceLocation;
        this.symbol = symbol;
        this.patternMatches = ImmutableList.copyOf(patternMatches);
        this.body = body;
    }

    public PatternCase accumulateDependencies(DependencyAccumulator state) {
        return state.keep(withMatches(patternMatches.stream().map(match -> match.accumulateDependencies(state)).collect(toList()))
            .withBody(body.accumulateDependencies(state)));
    }

    public PatternCase accumulateNames(NameAccumulator state) {
        return state.scoped(this, () ->
            withMatches(patternMatches.stream()
                .map(match -> match.accumulateNames(state))
                .collect(toList()))
            .withBody(body.accumulateNames(state)));
    }

    public PatternCase bindMethods(TypeChecker state) {
        return state.scoped(this, () ->
            withMatches(patternMatches.stream()
                .map(match -> match.bindMethods(state))
                .collect(toList()))
            .withBody(body.bindMethods(state)));
    }

    public PatternCase bindTypes(TypeChecker state) {
        return state.scoped(this, () ->
            withMatches(patternMatches.stream()
                .map(match -> match.bindTypes(state))
                .collect(toList()))
            .withBody(body.bindTypes(state)));
    }

    public PatternCase checkTypes(TypeChecker state) {
        return state.scoped(this, () -> {
            patternMatches.stream()
                .map(PatternMatch::getType)
                .forEach(state::specialize);
            try {
                return withMatches(patternMatches.stream()
                    .map(match -> match.checkTypes(state))
                    .collect(toList()))
                    .withBody(body.checkTypes(state));
            } finally {
                patternMatches.stream()
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
                patternMatches.forEach(match -> append(match.generateBytecode(state)));
                append(body.generateBytecode(state));
                go_to(state.endCase());
                state.endMatches();
            });
        }};
    }

    public int getArity() {
        return patternMatches.size();
    }

    public Value getBody() {
        return body;
    }

    @Override
    public Definition getDefinition() {
        return scopeDef(sourceLocation, symbol);
    }

    public DefinitionReference getReference() {
        return scopeRef(symbol);
    }

    public SourceLocation getSourceLocation() {
        return sourceLocation;
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
            List<PatternMatch> boundMatches = patternMatches.stream()
                .map(match -> match.bind("#" + counter.getAndIncrement(), state.scope()))
                .collect(toList());
            return withSymbol(state.reserveSymbol())
                .withMatches(boundMatches)
                .withBody(body.parsePrecedence(state).unwrap());
        });
    }

    public PatternCase qualifyNames(ScopedNameQualifier state) {
        return state.scoped(this, () -> withMatches(patternMatches.stream()
            .map(match -> match.qualifyNames(state))
            .collect(toList()))
            .withBody(body.qualifyNames(state)));
    }

    public PatternCase withBody(Value body) {
        return new PatternCase(sourceLocation, symbol, patternMatches, body);
    }

    public PatternCase withMatches(List<PatternMatch> matches) {
        return new PatternCase(sourceLocation, symbol, matches, body);
    }

    public PatternCase withType(Type type) {
        return new PatternCase(sourceLocation, symbol, patternMatches, body.withType(type));
    }

    private PatternCase withSymbol(Symbol symbol) {
        return new PatternCase(sourceLocation, symbol, patternMatches, body);
    }

    public static class Builder implements SyntaxBuilder<PatternCase> {

        private Optional<SourceLocation>     sourceLocation = Optional.empty();
        private Optional<Symbol>             symbol      = Optional.empty();
        private Optional<List<PatternMatch>> matches     = Optional.empty();
        private Optional<Value>              body        = Optional.empty();

        @Override
        public PatternCase build() {
            return new PatternCase(
                require(sourceLocation, "Source location"),
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
