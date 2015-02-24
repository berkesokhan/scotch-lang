package scotch.compiler.syntax.value;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static scotch.compiler.syntax.builder.BuilderUtil.require;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import lombok.EqualsAndHashCode;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.NameQualifier;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.text.SourceRange;

@EqualsAndHashCode(callSuper = false)
public class DataConstructor extends Value {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceRange sourceRange;
    private final Symbol      symbol;
    private final List<Value> arguments;
    private final Type        type;

    DataConstructor(SourceRange sourceRange, Symbol symbol, Type type, List<Value> arguments) {
        this.sourceRange = sourceRange;
        this.symbol = symbol;
        this.arguments = ImmutableList.copyOf(arguments);
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
        return withArguments(state.bindMethods(arguments));
    }

    @Override
    public Value bindTypes(TypeChecker state) {
        return withType(state.generate(type))
            .withArguments(state.bindTypes(arguments));
    }

    @Override
    public Value checkTypes(TypeChecker state) {
        return withArguments(state.checkTypes(arguments));
    }

    @Override
    public Value defineOperators(OperatorAccumulator state) {
        return withArguments(state.defineValueOperators(arguments));
    }

    private DataConstructor withArguments(List<Value> arguments) {
        return new DataConstructor(sourceRange, symbol, type, arguments);
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        return new CodeBlock() {{
            newobj(state.getDataConstructorClass(symbol));
            dup();
            arguments.forEach(argument -> append(argument.generateBytecode(state)));
            List<Class<?>> parameters = arguments.stream()
                .map(Value::getType)
                .map(Type::getJavaType)
                .collect(toList());
            invokespecial(state.getDataConstructorClass(symbol), "<init>", sig(void.class, parameters.toArray(new Class<?>[parameters.size()])));
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
    public Value parsePrecedence(PrecedenceParser state) {
        return withArguments(arguments.stream()
            .map(argument -> argument.parsePrecedence(state))
            .collect(toList()));
    }

    @Override
    public Value qualifyNames(NameQualifier state) {
        return withArguments(state.qualifyValueNames(arguments))
            .withType(type.qualifyNames(state));
    }

    @Override
    public String toString() {
        return symbol.toString() + "(" + arguments.stream().map(Object::toString).collect(joining(", ")) + ")";
    }

    @Override
    public DataConstructor withType(Type type) {
        return new DataConstructor(sourceRange, symbol, type, arguments);
    }

    public static class Builder implements SyntaxBuilder<DataConstructor> {

        private Optional<SourceRange> sourceRange;
        private Optional<Symbol>      symbol;
        private List<Value>           arguments;
        private Optional<Type>        type;

        public Builder() {
            sourceRange = Optional.empty();
            symbol = Optional.empty();
            arguments = new ArrayList<>();
            type = Optional.empty();
        }

        @Override
        public DataConstructor build() {
            return new DataConstructor(
                require(sourceRange, "Source range"),
                require(symbol, "Constructor symbol"),
                require(type, "Constructor type"),
                arguments
            );
        }

        public Builder withArguments(List<Value> arguments) {
            this.arguments.addAll(arguments);
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
