package scotch.compiler.syntax.pattern;

import static lombok.AccessLevel.PACKAGE;
import static scotch.compiler.error.SymbolNotFoundError.symbolNotFound;
import static scotch.symbol.Symbol.unqualified;
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
import scotch.symbol.Operator;
import scotch.symbol.Symbol;
import scotch.symbol.type.Type;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.syntax.value.Identifier;
import scotch.compiler.text.SourceLocation;
import scotch.compiler.util.Either;
import scotch.compiler.util.Pair;

@AllArgsConstructor(access = PACKAGE)
@EqualsAndHashCode(callSuper = false, doNotUseGetters = true)
@ToString(exclude = "sourceLocation", doNotUseGetters = true)
public class CaptureMatch extends PatternMatch {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceLocation   sourceLocation;
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
        if (this.argument.isPresent() && !argument.equals(this.argument.get())) {
            throw new IllegalStateException("Can't rebind-bind capture match argument '" + this.argument.get() + "' to argument '" + argument + "'");
        } else {
            return Patterns.capture(sourceLocation, Optional.of(argument), symbol, type);
        }
    }

    @Override
    public PatternMatch bindMethods(TypeChecker state) {
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
                    state.error(typeError(unification, sourceLocation));
                    return type;
                })))
            .orElseGet(() -> {
                state.error(symbolNotFound(unqualified(getArgument()), sourceLocation));
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
    public SourceLocation getSourceLocation() {
        return sourceLocation;
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

    public CaptureMatch withSourceLocation(SourceLocation sourceLocation) {
        return new CaptureMatch(sourceLocation, argument, symbol, type);
    }

    @Override
    public PatternMatch withType(Type type) {
        return new CaptureMatch(sourceLocation, argument, symbol, type);
    }

    public static class Builder implements SyntaxBuilder<CaptureMatch> {

        private Optional<SourceLocation> sourceLocation = Optional.empty();
        private Optional<Symbol>         symbol      = Optional.empty();
        private Optional<Type>           type        = Optional.empty();

        private Builder() {
            // intentionally empty
        }

        @Override
        public CaptureMatch build() {
            return Patterns.capture(
                require(sourceLocation, "Source location"),
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
        public Builder withSourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = Optional.of(sourceLocation);
            return this;
        }
    }
}
