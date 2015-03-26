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
import scotch.compiler.intermediate.IntermediateGenerator;
import scotch.compiler.intermediate.IntermediateValue;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.text.SourceLocation;
import scotch.symbol.Symbol;
import scotch.symbol.type.Type;

@EqualsAndHashCode(callSuper = false)
public class DataConstructor extends Value {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceLocation sourceLocation;
    private final Symbol         symbol;
    private final List<Value>    arguments;
    private final Type           type;

    DataConstructor(SourceLocation sourceLocation, Symbol symbol, Type type, List<Value> arguments) {
        this.sourceLocation = sourceLocation;
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
    public IntermediateValue generateIntermediateCode(IntermediateGenerator state) {
        throw new UnsupportedOperationException(); // TODO
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
        return new DataConstructor(sourceLocation, symbol, type, arguments);
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
    public SourceLocation getSourceLocation() {
        return sourceLocation;
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
    public Value qualifyNames(ScopedNameQualifier state) {
        return withArguments(state.qualifyValueNames(arguments))
            .withType(type.qualifyNames(state));
    }

    @Override
    public String toString() {
        return symbol.toString() + "(" + arguments.stream().map(Object::toString).collect(joining(", ")) + ")";
    }

    @Override
    public DataConstructor withType(Type type) {
        return new DataConstructor(sourceLocation, symbol, type, arguments);
    }

    public static class Builder implements SyntaxBuilder<DataConstructor> {

        private Optional<SourceLocation> sourceLocation;
        private Optional<Symbol>         symbol;
        private List<Value>              arguments;
        private Optional<Type>           type;

        public Builder() {
            sourceLocation = Optional.empty();
            symbol = Optional.empty();
            arguments = new ArrayList<>();
            type = Optional.empty();
        }

        @Override
        public DataConstructor build() {
            return new DataConstructor(
                require(sourceLocation, "Source location"),
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
}
