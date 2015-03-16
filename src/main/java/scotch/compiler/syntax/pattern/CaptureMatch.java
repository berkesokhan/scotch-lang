package scotch.compiler.syntax.pattern;

import static lombok.AccessLevel.PACKAGE;
import static scotch.compiler.error.SymbolNotFoundError.symbolNotFound;
import static scotch.compiler.symbol.Symbol.unqualified;
import static scotch.compiler.syntax.TypeError.typeError;
import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.compiler.util.Either.right;
import static scotch.compiler.util.Pair.pair;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.symbol.Operator;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.syntax.value.Identifier;
import scotch.compiler.syntax.value.InstanceMap;
import scotch.compiler.text.SourceRange;
import scotch.compiler.util.Either;
import scotch.compiler.util.Pair;

@AllArgsConstructor(access = PACKAGE)
@EqualsAndHashCode(callSuper = false, doNotUseGetters = true)
@ToString(exclude = "sourceRange")
public class CaptureMatch extends PatternMatch {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceRange      sourceRange;
    private final Optional<String> argument;
    private final Symbol           symbol;
    private final Type             type;

    @Override
    public PatternMatch accumulateDependencies(DependencyAccumulator state) {
        return this;
    }

    @Override
    public PatternMatch accumulateNames(NameAccumulator state) {
        state.defineValue(symbol, type);
        state.specialize(type);
        return this;
    }

    @Override
    public Either<PatternMatch, CaptureMatch> asCapture() {
        return right(this);
    }

    @Override
    public Optional<Pair<CaptureMatch, Operator>> asOperator(Scope scope) {
        return scope.qualify(symbol)
            .flatMap(scope::getOperator)
            .map(operator -> pair(this, operator));
    }

    @Override
    public PatternMatch bind(String argument, Scope scope) {
        if (this.argument.isPresent()) {
            throw new IllegalStateException();
        } else {
            return Patterns.capture(sourceRange, Optional.of(argument), symbol, type);
        }
    }

    @Override
    public PatternMatch bindMethods(TypeChecker state, InstanceMap instances) {
        return this;
    }

    @Override
    public PatternMatch bindTypes(TypeChecker state) {
        return withType(state.generate(type));
    }

    @Override
    public PatternMatch checkTypes(TypeChecker state) {
        Scope scope = state.scope();
        state.addLocal(symbol);
        return scope.getValue(unqualified(getArgument()))
            .map(argument -> withType(scope.generate(type)
                .unify(argument, scope)
                .orElseGet(unification -> {
                    state.error(typeError(unification, sourceRange));
                    return type;
                })))
            .orElseGet(() -> {
                state.error(symbolNotFound(unqualified(getArgument()), sourceRange));
                return this;
            });
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        return new CodeBlock() {{
            state.addMatch(getName());
            aload(state.getVariable(getArgument()));
            astore(state.getVariable(getName()));
        }};
    }

    public String getArgument() {
        return argument.orElseThrow(IllegalStateException::new);
    }

    public String getName() {
        return symbol.getCanonicalName();
    }

    @Override
    public SourceRange getSourceRange() {
        return sourceRange;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public boolean isOperator(Scope scope) {
        return scope.isOperator(symbol);
    }

    @Override
    public PatternMatch qualifyNames(ScopedNameQualifier state) {
        return this;
    }

    public CaptureMatch withSourceRange(SourceRange sourceRange) {
        return new CaptureMatch(sourceRange, argument, symbol, type);
    }

    @Override
    public PatternMatch withType(Type type) {
        return new CaptureMatch(sourceRange, argument, symbol, type);
    }

    public static class Builder implements SyntaxBuilder<CaptureMatch> {

        private Optional<SourceRange> sourceRange = Optional.empty();
        private Optional<Symbol>      symbol      = Optional.empty();
        private Optional<Type>        type        = Optional.empty();

        private Builder() {
            // intentionally empty
        }

        @Override
        public CaptureMatch build() {
            return Patterns.capture(
                require(sourceRange, "Source range"),
                Optional.empty(),
                require(symbol, "Capture symbol"),
                require(type, "Capture type")
            );
        }

        public Builder withIdentifier(Identifier identifier) {
            this.symbol = Optional.of(identifier.getSymbol());
            this.type = Optional.of(identifier.getType());
            return this;
        }

        @Override
        public Builder withSourceRange(SourceRange sourceRange) {
            this.sourceRange = Optional.of(sourceRange);
            return this;
        }
    }
}
