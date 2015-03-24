package scotch.compiler.syntax.value;

import static java.util.Collections.reverse;
import static java.util.stream.Collectors.toList;
import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static scotch.compiler.syntax.TypeError.typeError;
import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.compiler.syntax.definition.Definitions.scopeDef;
import static scotch.compiler.syntax.reference.DefinitionReference.scopeRef;
import static scotch.compiler.syntax.value.Values.matcher;
import static scotch.symbol.type.Types.fn;
import static scotch.symbol.type.Unification.unified;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import com.google.common.collect.ImmutableList;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import me.qmx.jitescript.CodeBlock;
import me.qmx.jitescript.LambdaBlock;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.syntax.Scoped;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.definition.Definition;
import scotch.compiler.syntax.pattern.PatternCase;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.text.SourceLocation;
import scotch.runtime.Applicable;
import scotch.runtime.Callable;
import scotch.symbol.Symbol;
import scotch.symbol.type.Type;

@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "sourceLocation", doNotUseGetters = true)
public class PatternMatcher extends Value implements Scoped {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceLocation    sourceLocation;
    private final Symbol            symbol;
    private final List<Argument>    arguments;
    private final List<PatternCase> patternCases;
    private final Type              type;

    PatternMatcher(SourceLocation sourceLocation, Symbol symbol, List<Argument> arguments, List<PatternCase> patternCases, Type type) {
        this.sourceLocation = sourceLocation;
        this.symbol = symbol;
        this.arguments = ImmutableList.copyOf(arguments);
        this.patternCases = ImmutableList.copyOf(patternCases);
        this.type = type;
    }

    @Override
    public Value accumulateDependencies(DependencyAccumulator state) {
        return state.keep(map(
            argument -> argument.accumulateDependencies(state),
            patternCase -> patternCase.accumulateDependencies(state)
        ));
    }

    @Override
    public Value accumulateNames(NameAccumulator state) {
        return state.scoped(this, () -> map(
            argument -> argument.accumulateNames(state),
            patternCase -> patternCase.accumulateNames(state)
        ));
    }

    @Override
    public Value bindMethods(TypeChecker state) {
        return withPatternCases(patternCases.stream()
            .map(matcher -> matcher.bindMethods(state))
            .collect(toList()));
    }

    @Override
    public Value bindTypes(TypeChecker state) {
        return map(argument -> argument.bindTypes(state), patternCase -> patternCase.bindTypes(state)).withType(state.generate(type));
    }

    @Override
    public Value checkTypes(TypeChecker state) {
        return encloseArguments(state, () -> {
            AtomicReference<Type> resultType = new AtomicReference<>(state.reserveType());
            List<PatternCase> patterns = patternCases.stream()
                .map(matcher -> matcher.checkTypes(state))
                .collect(toList());
            patterns = patterns.stream()
                .map(pattern -> pattern.withType(pattern.getType().unify(resultType.get(), state)
                    .map(unifiedType -> {
                        Type result = state.scope().generate(unifiedType);
                        resultType.set(result);
                        return unified(unifiedType);
                    })
                    .orElseGet(unification -> {
                        state.error(typeError(unification.flip(), pattern.getSourceLocation()));
                        return pattern.getType();
                    })))
                .collect(toList());
            return withPatternCases(patterns).withType(calculateType(resultType.get()));
        });
    }

    @Override
    public Value defineOperators(OperatorAccumulator state) {
        return state.scoped(this, () -> map(
            argument -> argument,
            patternCase -> patternCase.defineOperators(state)
        ));
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        return state.enclose(this, () -> curry().generateBytecode(state));
    }

    public List<Argument> getArguments() {
        return arguments;
    }

    @Override
    public Definition getDefinition() {
        return scopeDef(sourceLocation, symbol);
    }

    @Override
    public DefinitionReference getReference() {
        return scopeRef(symbol);
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Value parsePrecedence(PrecedenceParser state) {
        Symbol s = symbol.getMemberNames().size() == 1 ? state.reserveSymbol() : symbol;
        return state.named(s, () -> state.scoped(this, () -> withSymbol(s).withPatternCases(patternCases.stream()
            .map(matcher -> matcher.parsePrecedence(state))
            .collect(toList()))));
    }

    @Override
    public Value qualifyNames(ScopedNameQualifier state) {
        return state.named(symbol, () -> state.scoped(this, () -> map(
            argument -> argument.qualifyNames(state),
            patternCase -> patternCase.qualifyNames(state)
        )));
    }

    @Override
    public Value unwrap() {
        return withPatternCases(
            patternCases.stream()
                .map(matcher -> matcher.withBody(matcher.getBody().unwrap()))
                .collect(toList())
        );
    }

    @Override
    public WithArguments withArguments() {
        return WithArguments.withArguments(this);
    }

    public PatternMatcher withArguments(List<Argument> arguments) {
        return new PatternMatcher(sourceLocation, symbol, arguments, patternCases, type);
    }

    public PatternMatcher withPatternCases(List<PatternCase> patternCases) {
        return new PatternMatcher(sourceLocation, symbol, arguments, patternCases, type);
    }

    public PatternMatcher withSourceLocation(SourceLocation sourceLocation) {
        return new PatternMatcher(sourceLocation, symbol, arguments, patternCases, type);
    }

    public PatternMatcher withSymbol(Symbol symbol) {
        return new PatternMatcher(sourceLocation, symbol, arguments, patternCases, type);
    }

    @Override
    public PatternMatcher withType(Type type) {
        return new PatternMatcher(sourceLocation, symbol, arguments, patternCases, type);
    }

    private Type calculateType(Type returnType) {
        List<Argument> args = new ArrayList<>(arguments);
        reverse(args);
        return args.stream()
            .map(Argument::getType)
            .reduce(returnType, (result, arg) -> fn(arg, result));
    }

    private CurriedPattern curry() {
        return curry_(new ArrayDeque<>(arguments));
    }

    private CurriedPattern curry_(Deque<Argument> arguments) {
        if (arguments.isEmpty()) {
            return new CurriedBody(patternCases);
        } else {
            return new CurriedLambda(arguments.pop(), curry_(arguments));
        }
    }

    private PatternMatcher encloseArguments(TypeChecker state, Supplier<PatternMatcher> supplier) {
        return state.enclose(this, () -> {
            arguments.stream()
                .map(Argument::getType)
                .forEach(state::specialize);
            arguments.stream()
                .map(Argument::getSymbol)
                .forEach(state::addLocal);
            try {
                return supplier.get();
            } finally {
                arguments.stream()
                    .map(Argument::getType)
                    .forEach(state::generalize);
            }
        });
    }

    private PatternMatcher map(Function<Argument, Argument> argumentMapper, Function<PatternCase, PatternCase> patternCaseMapper) {
        return new PatternMatcher(
            sourceLocation, symbol,
            arguments.stream().map(argumentMapper).collect(toList()),
            patternCases.stream().map(patternCaseMapper).collect(toList()),
            type
        );
    }

    private interface CurriedPattern {

        CodeBlock generateBytecode(BytecodeGenerator state);

        Type getType();
    }

    public static class Builder implements SyntaxBuilder<PatternMatcher> {

        private Optional<SourceLocation>    sourceLocation = Optional.empty();
        private Optional<List<Argument>>    arguments      = Optional.empty();
        private Optional<List<PatternCase>> patternCases   = Optional.empty();
        private Optional<Type>              type           = Optional.empty();
        private Optional<Symbol>            symbol         = Optional.empty();

        private Builder() {
            // intentionally empty
        }

        @Override
        public PatternMatcher build() {
            return matcher(
                require(sourceLocation, "Source location"),
                require(symbol, "Symbol"),
                require(type, "Pattern type"),
                require(arguments, "Arguments"),
                require(patternCases, "Pattern cases")
            );
        }

        public Builder withArguments(List<Argument> arguments) {
            this.arguments = Optional.of(arguments);
            return this;
        }

        public Builder withPatterns(List<PatternCase> patterns) {
            this.patternCases = Optional.of(patterns);
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

        public Builder withType(Type type) {
            this.type = Optional.of(type);
            return this;
        }
    }

    private static final class CurriedBody implements CurriedPattern {

        private final List<PatternCase> patternCases;

        public CurriedBody(List<PatternCase> patternCases) {
            this.patternCases = ImmutableList.copyOf(patternCases);
        }

        @Override
        public CodeBlock generateBytecode(BytecodeGenerator state) {
            return new CodeBlock() {{
                state.beginCases(patternCases.size());
                patternCases.forEach(matcher -> append(matcher.generateBytecode(state)));
                label(state.endCases());
            }};
        }

        @Override
        public Type getType() {
            return patternCases.get(0).getType();
        }
    }

    @AllArgsConstructor
    private static final class CurriedLambda implements CurriedPattern {

        private final Argument       argument;
        private final CurriedPattern body;

        @Override
        public CodeBlock generateBytecode(BytecodeGenerator state) {
            return new CodeBlock() {{
                append(state.captureLambda(argument.getName()));
                lambda(state.currentClass(), new LambdaBlock(state.reserveLambda()) {{
                    function(p(Applicable.class), "apply", sig(Callable.class, Callable.class));
                    capture(state.getLambdaCaptureTypes());
                    delegateTo(ACC_STATIC, sig(state.typeOf(body.getType()), state.getLambdaType()), new CodeBlock() {{
                        append(body.generateBytecode(state));
                        areturn();
                    }});
                }});
                state.releaseLambda(argument.getName());
            }};
        }

        @Override
        public Type getType() {
            return fn(argument.getType(), body.getType());
        }
    }
}
