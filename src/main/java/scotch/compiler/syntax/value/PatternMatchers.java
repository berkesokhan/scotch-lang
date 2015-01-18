package scotch.compiler.syntax.value;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.TypeError.typeError;
import static scotch.util.StringUtil.stringify;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.Unification;
import scotch.compiler.symbol.Unification.UnificationVisitor;
import scotch.compiler.symbol.Unification.Unified;
import scotch.compiler.syntax.BytecodeGenerator;
import scotch.compiler.syntax.DependencyAccumulator;
import scotch.compiler.syntax.NameAccumulator;
import scotch.compiler.syntax.NameQualifier;
import scotch.compiler.syntax.OperatorDefinitionParser;
import scotch.compiler.syntax.PrecedenceParser;
import scotch.compiler.syntax.TypeChecker;
import scotch.compiler.text.SourceRange;

public class PatternMatchers extends Value {

    private final SourceRange          sourceRange;
    private final List<PatternMatcher> matchers;
    private final Type                 type;

    PatternMatchers(SourceRange sourceRange, List<PatternMatcher> matchers, Type type) {
        this.sourceRange = sourceRange;
        this.matchers = matchers;
        this.type = type;
    }

    @Override
    public Value accumulateDependencies(DependencyAccumulator state) {
        return withMatchers(matchers.stream()
            .map(matcher -> matcher.accumulateDependencies(state))
            .collect(toList()));
    }

    @Override
    public Value accumulateNames(NameAccumulator state) {
        return withMatchers(matchers.stream()
            .map(matcher -> matcher.accumulateNames(state))
            .collect(toList()));
    }

    @Override
    public Value bindMethods(TypeChecker state) {
        return withMatchers(matchers.stream()
            .map(matcher -> matcher.bindMethods(state))
            .collect(toList()));
    }

    @Override
    public Value bindTypes(TypeChecker state) {
        return withMatchers(matchers.stream()
            .map(matcher -> matcher
                .withMatches(matcher.getMatches().stream().map(match -> match.bindTypes(state)).collect(toList()))
                .withBody(matcher.getBody().bindTypes(state))
                .withType(state.generate(matcher.getType())))
            .collect(toList()))
            .withType(state.generate(type));
    }

    @Override
    public Value checkTypes(TypeChecker state) {
        List<PatternMatcher> patterns = matchers.stream()
            .map(matcher -> matcher.analyzeTypes(state))
            .collect(toList());
        AtomicReference<Type> type = new AtomicReference<>(state.reserveType());
        patterns = patterns.stream()
            .map(pattern -> pattern.getType().unify(type.get(), state).accept(new UnificationVisitor<PatternMatcher>() {
                @Override
                public PatternMatcher visit(Unified unified) {
                    Type result = state.scope().generate(unified.getUnifiedType());
                    type.set(result);
                    return pattern.withType(result);
                }

                @Override
                public PatternMatcher visitOtherwise(Unification unification) {
                    state.error(typeError(unification.flip(), pattern.getSourceRange()));
                    return pattern;
                }
            }))
            .collect(toList());
        return withMatchers(patterns).withType(type.get());
    }

    @Override
    public Value defineOperators(OperatorDefinitionParser state) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof PatternMatchers) {
            PatternMatchers other = (PatternMatchers) o;
            return Objects.equals(matchers, other.matchers)
                && Objects.equals(type, other.type);
        } else {
            return false;
        }
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        return new CodeBlock() {{
            matchers.forEach(matcher -> state.generate(matcher, () -> {
                state.beginMatches();
                matcher.getMatches().forEach(match -> append(match.generateBytecode(state)));
                append(matcher.getBody().generateBytecode(state));
                state.endMatches();
            }));
        }};
    }

    public List<PatternMatcher> getMatchers() {
        return matchers;
    }

    @Override
    public SourceRange getSourceRange() {
        return sourceRange;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(matchers);
    }

    @Override
    public Value parsePrecedence(PrecedenceParser state) {
        return withMatchers(matchers.stream()
            .map(matcher -> matcher.parsePrecedence(state))
            .collect(toList()));
    }

    @Override
    public Value qualifyNames(NameQualifier state) {
        return withMatchers(matchers.stream()
            .map(matcher -> matcher.qualifyNames(state))
            .collect(toList()));
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + matchers + ")";
    }

    @Override
    public Value unwrap() {
        return withMatchers(
            matchers.stream()
                .map(matcher -> matcher.withBody(matcher.getBody().unwrap()))
                .collect(toList())
        );
    }

    public PatternMatchers withMatchers(List<PatternMatcher> matchers) {
        return new PatternMatchers(sourceRange, matchers, type);
    }

    public PatternMatchers withSourceRange(SourceRange sourceRange) {
        return new PatternMatchers(sourceRange, matchers, type);
    }

    @Override
    public PatternMatchers withType(Type type) {
        return new PatternMatchers(sourceRange, matchers, type);
    }
}
