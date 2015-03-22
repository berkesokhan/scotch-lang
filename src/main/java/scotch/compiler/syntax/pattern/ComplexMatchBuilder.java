package scotch.compiler.syntax.pattern;

import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.compiler.syntax.pattern.Patterns.field;
import static scotch.compiler.text.TextUtil.repeat;
import static scotch.symbol.Symbol.qualified;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.text.SourceLocation;
import scotch.symbol.Symbol;
import scotch.symbol.util.SymbolGenerator;

public class ComplexMatchBuilder implements SyntaxBuilder<PatternMatch> {

    public static ComplexMatchBuilder complexMatchBuilder(SymbolGenerator symbolGenerator) {
        return new ComplexMatchBuilder(symbolGenerator);
    }

    private final SymbolGenerator          symbolGenerator;
    private final List<PatternMatch>       patternMatches;
    private       Optional<SourceLocation> sourceLocation;
    private       State                    state;

    private ComplexMatchBuilder(SymbolGenerator symbolGenerator) {
        this.symbolGenerator = symbolGenerator;
        this.patternMatches = new ArrayList<>();
        this.sourceLocation = Optional.empty();
        this.state = new DefaultState();
    }

    @Override
    public PatternMatch build() {
        return state.build();
    }

    public ComplexMatchBuilder withPatternMatch(PatternMatch patterMatch) {
        patternMatches.add(patterMatch);
        return this;
    }

    @Override
    public ComplexMatchBuilder withSourceLocation(SourceLocation sourceLocation) {
        this.sourceLocation = Optional.of(sourceLocation);
        return this;
    }

    public void tuplize() {
        state.tuplize();
    }

    private interface State {

        PatternMatch build();

        void tuplize();
    }

    private final class DefaultState implements State {

        @Override
        public PatternMatch build() {
            UnshuffledStructureMatch.Builder structureMatch = UnshuffledStructureMatch.builder()
                .withSourceLocation(require(sourceLocation, "Source location"))
                .withType(symbolGenerator.reserveType());
            patternMatches.forEach(structureMatch::withPatternMatch);
            return structureMatch.build();
        }

        @Override
        public void tuplize() {
            state = new TupleState();
        }
    }

    private final class TupleState implements State {

        @Override
        public PatternMatch build() {
            Symbol constructor = qualified("scotch.data.tuple", "(" + repeat(",", patternMatches.size() - 1) + ")");
            TupleMatch.Builder structureMatch = TupleMatch.builder()
                .withSourceLocation(require(sourceLocation, "Source location"))
                .withType(symbolGenerator.reserveType())
                .withConstructor(constructor);
            patternMatches.stream()
                .map(match -> field(match.getSourceLocation(), Optional.empty(), Optional.empty(), symbolGenerator.reserveType(), match))
                .forEach(structureMatch::withField);
            return structureMatch.build();
        }

        @Override
        public void tuplize() {
            // noop
        }
    }
}
