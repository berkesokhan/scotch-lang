package scotch.compiler.syntax.definition;

import static me.qmx.jitescript.util.CodegenUtils.sig;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static scotch.compiler.syntax.TypeError.typeError;
import static scotch.compiler.syntax.reference.DefinitionReference.valueRef;
import static scotch.data.either.Either.right;
import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import java.util.Optional;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.symbol.Symbol;
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
import scotch.compiler.syntax.reference.ValueReference;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.text.SourceRange;
import scotch.data.either.Either;

public class ValueDefinition extends Definition {

    private final SourceRange sourceRange;
    private       Symbol      symbol;
    private       Value       body;
    private       Type        type;

    ValueDefinition(SourceRange sourceRange, Symbol symbol, Value body, Type type) {
        this.sourceRange = sourceRange;
        this.symbol = symbol;
        this.body = body;
        this.type = type;
    }

    @Override
    public Definition accumulateDependencies(DependencyAccumulator state) {
        return state.scoped(this, () -> {
            state.pushSymbol(symbol);
            try {
                return withBody(body.accumulateDependencies(state));
            } finally {
                state.popSymbol();
            }
        });
    }

    @Override
    public Definition accumulateNames(NameAccumulator state) {
        state.defineValue(symbol, type);
        state.specialize(type);
        return state.scoped(this, () -> withBody(body.accumulateNames(state)));
    }

    @Override
    public Either<Definition, ValueDefinition> asValue() {
        return right(this);
    }

    @Override
    public Definition bindTypes(TypeChecker state) {
        return withBody(body.bindTypes(state)).withType(state.generate(type));
    }

    @Override
    public Definition checkTypes(TypeChecker state) {
        return state.enclose(this, () -> {
            Value body = this.body.checkTypes(state);
            Type type = state.getType(this);
            return type.unify(body.getType(), state.scope()).accept(new UnificationVisitor<Definition>() {
                @Override
                public Definition visit(Unified unified) {
                    Type unifiedType = state.scope().generate(unified.getUnifiedType());
                    ValueDefinition result = withBody(body).withType(unifiedType);
                    state.redefine(result);
                    return state.bind(result);
                }

                @Override
                public Definition visitOtherwise(Unification unification) {
                    state.error(typeError(unification, sourceRange));
                    return withBody(body).withType(type);
                }
            });
        });
    }

    @Override
    public Definition defineOperators(OperatorDefinitionParser state) {
        return state.scoped(this, () -> withBody(body.defineOperators(state)));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof ValueDefinition) {
            ValueDefinition other = (ValueDefinition) o;
            return Objects.equals(symbol, other.symbol)
                && Objects.equals(body, other.body)
                && Objects.equals(type, other.type);
        } else {
            return false;
        }
    }

    @Override
    public void generateBytecode(BytecodeGenerator state) {
        state.generate(this, () -> state.method(getMethodName(), ACC_STATIC | ACC_PUBLIC, sig(state.typeOf(type)), new CodeBlock() {{
            annotate(Value.class).value("memberName", symbol.getSimpleName());
            markLine(this);
            append(body.generateBytecode(state));
            areturn();
        }}));
    }

    public Value getBody() {
        return body;
    }

    public String getMethodName() {
        return symbol.unqualify().getMethodName();
    }

    @Override
    public ValueReference getReference() {
        return valueRef(symbol);
    }

    public String getSignature() {
        return type.getSignature();
    }

    public SourceRange getSourceRange() {
        return sourceRange;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public Type getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, body, type);
    }

    @Override
    public Optional<Definition> parsePrecedence(PrecedenceParser state) {
        return Optional.of(state.scoped(this, () -> withBody(body.parsePrecedence(state).unwrap())));
    }

    @Override
    public Definition qualifyNames(NameQualifier state) {
        return state.scoped(this, () -> withBody(body.qualifyNames(state)));
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + symbol + " :: " + type + ")";
    }

    public ValueDefinition withBody(Value body) {
        return new ValueDefinition(sourceRange, symbol, body, type);
    }

    public ValueDefinition withSourceRange(SourceRange sourceRange) {
        return new ValueDefinition(sourceRange, symbol, body, type);
    }

    public ValueDefinition withType(Type type) {
        return new ValueDefinition(sourceRange, symbol, body, type);
    }
}
