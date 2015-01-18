package scotch.compiler.syntax.value;

import static scotch.compiler.symbol.Symbol.unqualified;
import static scotch.compiler.syntax.TypeError.typeError;
import static scotch.data.either.Either.right;
import static scotch.data.tuple.TupleValues.tuple2;
import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import java.util.Optional;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.symbol.NameQualifier;
import scotch.compiler.symbol.Operator;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.Unification;
import scotch.compiler.symbol.Unification.UnificationVisitor;
import scotch.compiler.symbol.Unification.Unified;
import scotch.compiler.syntax.BytecodeGenerator;
import scotch.compiler.syntax.DependencyAccumulator;
import scotch.compiler.syntax.NameAccumulator;
import scotch.compiler.syntax.TypeChecker;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.text.SourceRange;
import scotch.data.either.Either;
import scotch.data.tuple.Tuple2;

public class CaptureMatch extends PatternMatch {

    private final SourceRange      sourceRange;
    private final Optional<String> argument;
    private final Symbol           symbol;
    private final Type             type;

    CaptureMatch(SourceRange sourceRange, Optional<String> argument, Symbol symbol, Type type) {
        this.sourceRange = sourceRange;
        this.argument = argument;
        this.symbol = symbol;
        this.type = type;
    }

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
    public Optional<Tuple2<CaptureMatch, Operator>> asOperator(Scope scope) {
        return scope.qualify(symbol)
            .map(scope::getOperator)
            .map(operator -> tuple2(this, operator));
    }

    @Override
    public PatternMatch bind(String argument) {
        if (this.argument.isPresent()) {
            throw new IllegalStateException();
        } else {
            return capture(sourceRange, Optional.of(argument), symbol, type);
        }
    }

    @Override
    public PatternMatch bindTypes(TypeChecker state) {
        return withType(state.generate(type));
    }

    @Override
    public PatternMatch checkTypes(TypeChecker state) {
        Scope scope = state.scope();
        state.addLocal(symbol);
        return scope.generate(type)
            .unify(scope.getValue(unqualified(getArgument())), scope)
            .accept(new UnificationVisitor<PatternMatch>() {
                @Override
                public PatternMatch visit(Unified unified) {
                    return withType(unified.getUnifiedType());
                }

                @Override
                public PatternMatch visitOtherwise(Unification unification) {
                    state.error(typeError(unification, sourceRange));
                    return CaptureMatch.this;
                }
            });
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof CaptureMatch) {
            CaptureMatch other = (CaptureMatch) o;
            return Objects.equals(argument, other.argument)
                && Objects.equals(symbol, other.symbol)
                && Objects.equals(type, other.type);
        } else {
            return false;
        }
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
    public int hashCode() {
        return Objects.hash(argument, symbol, type);
    }

    @Override
    public boolean isOperator(Scope scope) {
        return scope.isOperator(symbol);
    }

    @Override
    public PatternMatch qualifyNames(NameQualifier state) {
        return this;
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + symbol + ")";
    }

    public CaptureMatch withSourceRange(SourceRange sourceRange) {
        return new CaptureMatch(sourceRange, argument, symbol, type);
    }

    @Override
    public PatternMatch withType(Type type) {
        return new CaptureMatch(sourceRange, argument, symbol, type);
    }
}
