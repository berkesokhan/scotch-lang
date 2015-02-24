package scotch.compiler.syntax.value;

import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static scotch.compiler.syntax.builder.BuilderUtil.require;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import me.qmx.jitescript.CodeBlock;
import me.qmx.jitescript.LambdaBlock;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.text.SourceRange;
import scotch.runtime.Callable;
import scotch.runtime.SuppliedThunk;

public class Constant extends Value {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceRange sourceRange;
    private final Symbol      symbol;
    private final Symbol      dataType;
    private final Type        type;

    Constant(SourceRange sourceRange, Symbol dataType, Symbol symbol, Type type) {
        this.sourceRange = sourceRange;
        this.dataType = dataType;
        this.symbol = symbol;
        this.type = type;
    }

    @Override
    public Value accumulateDependencies(DependencyAccumulator state) {
        return this;
    }

    @Override
    public Value accumulateNames(NameAccumulator state) {
        return this;
    }

    @Override
    public Value bindMethods(TypeChecker state) {
        return this;
    }

    @Override
    public Value bindTypes(TypeChecker state) {
        return withType(state.generate(type));
    }

    @Override
    public Value checkTypes(TypeChecker state) {
        return this;
    }

    @Override
    public Value defineOperators(OperatorAccumulator state) {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof Constant) {
            Constant other = (Constant) o;
            return Objects.equals(sourceRange, other.sourceRange)
                && Objects.equals(symbol, other.symbol)
                && Objects.equals(dataType, other.dataType)
                && Objects.equals(type, other.type);
        } else {
            return false;
        }
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        return new CodeBlock() {{
            String className = state.getDataConstructorClass(symbol);
            newobj(p(SuppliedThunk.class));
            dup();
            lambda(state.currentClass(), new LambdaBlock("$$constant$" + symbol.getMemberName()) {{
                function(p(Supplier.class), "get", sig(Object.class));
                specialize(sig(Callable.class));
                capture(new Class<?>[0]);
                delegateTo(ACC_STATIC | ACC_PRIVATE, sig(Callable.class), new CodeBlock() {{
                    newobj(className);
                    dup();
                    invokespecial(className, "<init>", sig(void.class));
                    areturn();
                }});
            }});
            invokespecial(p(SuppliedThunk.class), "<init>", sig(void.class, Supplier.class));
        }};
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
        return Objects.hash(symbol, dataType, type);
    }

    @Override
    public Value parsePrecedence(PrecedenceParser state) {
        return this;
    }

    @Override
    public Value qualifyNames(ScopedNameQualifier state) {
        return withType(type.qualifyNames(state));
    }

    @Override
    public String toString() {
        return symbol.toString();
    }

    @Override
    public Value withType(Type type) {
        return new Constant(sourceRange, dataType, symbol, type);
    }

    public static class Builder implements SyntaxBuilder<Constant> {

        private Optional<SourceRange> sourceRange;
        private Optional<Symbol>      dataType;
        private Optional<Symbol>      symbol;
        private Optional<Type>        type;

        private Builder() {
            sourceRange = Optional.empty();
            dataType = Optional.empty();
            symbol = Optional.empty();
            type = Optional.empty();
        }

        @Override
        public Constant build() {
            return new Constant(
                require(sourceRange, "Source range"),
                require(dataType, "Data type"),
                require(symbol, "Constant symbol"),
                require(type, "Constant type")
            );
        }

        public Builder withDataType(Symbol dataType) {
            this.dataType = Optional.of(dataType);
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

        public Builder withType(Type type) {
            this.type = Optional.of(type);
            return this;
        }
    }
}
