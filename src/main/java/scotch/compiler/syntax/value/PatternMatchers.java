package scotch.compiler.syntax.value;

import static java.util.stream.Collectors.toList;
import static scotch.compiler.symbol.Unification.unified;
import static scotch.compiler.syntax.TypeError.typeError;
import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.compiler.syntax.value.Values.patterns;
import static scotch.util.StringUtil.stringify;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.NameQualifier;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.text.SourceRange;

public class PatternMatchers extends Value {

    public static Builder builder() {
        return new Builder();
    }

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
            .map(matcher -> matcher.bindTypes(state))
            .collect(toList()))
            .withType(state.generate(type));
    }

    @Override
    public Value checkTypes(TypeChecker state) {
        List<PatternMatcher> patterns = matchers.stream()
            .map(matcher -> matcher.checkTypes(state))
            .collect(toList());
        AtomicReference<Type> type = new AtomicReference<>(state.reserveType());
        patterns = patterns.stream()
            .map(pattern -> pattern.withType(pattern.getType().unify(type.get(), state)
                .map(unifiedType -> {
                    Type result = state.scope().generate(unifiedType);
                    type.set(result);
                    return unified(unifiedType);
                })
                .orElseGet(unification -> {
                    state.error(typeError(unification.flip(), pattern.getSourceRange()));
                    return pattern.getType();
                })))
            .collect(toList());
        return withMatchers(patterns).withType(type.get());
    }

    @Override
    public Value defineOperators(OperatorAccumulator state) {
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
            state.beginCases(matchers.size());
            matchers.forEach(matcher -> append(matcher.generateBytecode(state)));
            label(state.endCases());
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

    public static class Builder implements SyntaxBuilder<PatternMatchers> {

        private Optional<SourceRange>          sourceRange;
        private Optional<List<PatternMatcher>> patterns;
        private Optional<Type>                 type;

        private Builder() {
            sourceRange = Optional.empty();
            patterns = Optional.empty();
            type = Optional.empty();
        }

        @Override
        public PatternMatchers build() {
            return patterns(
                require(sourceRange, "Source range"),
                require(type, "Pattern type"),
                require(patterns, "Patterns")
            );
        }

        public Builder withPatterns(List<PatternMatcher> patterns) {
            this.patterns = Optional.of(patterns);
            return this;
        }

        @Override
        public Builder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }

        public Builder withType(Type type) {
            this.type = Optional.of(type);
            return this;
        }
    }
}
