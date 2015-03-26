package scotch.compiler.syntax.value;

import static java.util.Collections.reverse;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static scotch.symbol.type.Types.fn;
import static scotch.compiler.syntax.builder.BuilderUtil.require;
import static scotch.compiler.syntax.definition.Definitions.scopeDef;
import static scotch.compiler.syntax.reference.DefinitionReference.scopeRef;
import static scotch.compiler.syntax.value.Values.fn;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import lombok.EqualsAndHashCode;
import me.qmx.jitescript.CodeBlock;
import me.qmx.jitescript.LambdaBlock;
import scotch.compiler.intermediate.IntermediateGenerator;
import scotch.compiler.intermediate.IntermediateValue;
import scotch.compiler.steps.BytecodeGenerator;
import scotch.compiler.steps.DependencyAccumulator;
import scotch.compiler.steps.NameAccumulator;
import scotch.compiler.steps.OperatorAccumulator;
import scotch.compiler.steps.PrecedenceParser;
import scotch.compiler.steps.ScopedNameQualifier;
import scotch.compiler.steps.TypeChecker;
import scotch.runtime.Applicable;
import scotch.runtime.Callable;
import scotch.symbol.Symbol;
import scotch.symbol.type.Type;
import scotch.compiler.syntax.Scoped;
import scotch.compiler.syntax.builder.SyntaxBuilder;
import scotch.compiler.syntax.definition.Definition;
import scotch.compiler.syntax.reference.ScopeReference;
import scotch.compiler.text.SourceLocation;

@EqualsAndHashCode(callSuper = false)
public class FunctionValue extends Value implements Scoped {

    public static Builder builder() {
        return new Builder();
    }

    private final SourceLocation sourceLocation;
    private final Symbol         symbol;
    private final List<Argument> arguments;
    private final Value          body;
    private final Optional<Type> type;

    FunctionValue(SourceLocation sourceLocation, Symbol symbol, List<Argument> arguments, Value body, Optional<Type> type) {
        this.sourceLocation = sourceLocation;
        this.symbol = symbol;
        this.arguments = ImmutableList.copyOf(arguments);
        this.body = body;
        this.type = type;
    }

    @Override
    public Value accumulateDependencies(DependencyAccumulator state) {
        return state.keep(withBody(body.accumulateDependencies(state)));
    }

    @Override
    public Value accumulateNames(NameAccumulator state) {
        return state.scoped(this, () -> withArguments(arguments.stream()
            .map(argument -> argument.accumulateNames(state))
            .collect(toList()))
            .withBody(body.accumulateNames(state)));
    }

    @Override
    public IntermediateValue generateIntermediateCode(IntermediateGenerator state) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public Value bindMethods(TypeChecker state) {
        return state.scoped(this, () -> withBody(body.bindMethods(state)));
    }

    @Override
    public Value bindTypes(TypeChecker state) {
        return withArguments(state.bindTypes(arguments))
            .withBody(body.bindTypes(state))
            .withType(state.generate(getType()));
    }

    @Override
    public Value checkTypes(TypeChecker state) {
        return state.enclose(this, () -> {
            arguments.stream()
                .map(Argument::getType)
                .forEach(state::specialize);
            arguments.stream()
                .map(Argument::getSymbol)
                .forEach(state::addLocal);
            try {
                return withBody(body.checkTypes(state));
            } finally {
                arguments.stream()
                    .map(Argument::getType)
                    .forEach(state::generalize);
            }
        });
    }

    public CurriedFunction curry() {
        return curry_(new ArrayDeque<>(arguments));
    }

    @Override
    public Value defineOperators(OperatorAccumulator state) {
        return state.scoped(this, () -> withBody(body.defineOperators(state)));
    }

    @Override
    public CodeBlock generateBytecode(BytecodeGenerator state) {
        return state.enclose(this, () -> curry().generateBytecode(state));
    }

    public List<Argument> getArguments() {
        return arguments;
    }

    public Value getBody() {
        return body;
    }

    @Override
    public Definition getDefinition() {
        return scopeDef(sourceLocation, symbol);
    }

    public ScopeReference getReference() {
        return scopeRef(symbol);
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
        return type.orElseGet(() -> {
            List<Argument> args = new ArrayList<>(arguments);
            reverse(args);
            return args.stream()
                .map(Argument::getType)
                .reduce(body.getType(), (result, arg) -> fn(arg, result));
        });
    }

    @Override
    public Value parsePrecedence(PrecedenceParser state) {
        Symbol s = symbol.getMemberNames().size() == 1 ? state.reserveSymbol() : symbol;
        return state.named(s, () -> state.scoped(this, () -> withSymbol(s).withBody(body.parsePrecedence(state))));
    }

    @Override
    public Value qualifyNames(ScopedNameQualifier state) {
        return state.named(symbol, () -> state.scoped(this, () -> new FunctionValue(
            sourceLocation,
            symbol,
            arguments.stream()
                .map(argument -> argument.qualifyNames(state))
                .collect(toList()),
            body.qualifyNames(state),
            type
        )));
    }

    @Override
    public String toString() {
        return "(" + arguments.stream().map(arg -> arg.getSymbol().toString()).collect(joining(", ")) + " -> " + body + ")";
    }

    @Override
    public WithArguments withArguments() {
        return WithArguments.withArguments(this);
    }

    public FunctionValue withArguments(List<Argument> arguments) {
        return fn(sourceLocation, symbol, arguments, body);
    }

    public FunctionValue withBody(Value body) {
        return fn(sourceLocation, symbol, arguments, body);
    }

    @Override
    public FunctionValue withType(Type type) {
        return new FunctionValue(sourceLocation, symbol, arguments, body, Optional.of(type));
    }

    private CurriedFunction curry_(Deque<Argument> args) {
        if (args.isEmpty()) {
            return new CurriedBody(body);
        } else {
            return new CurriedLambda(args.pop(), curry_(args));
        }
    }

    private FunctionValue withSymbol(Symbol symbol) {
        return fn(sourceLocation, symbol, arguments, body);
    }

    private interface CurriedFunction {

        CodeBlock generateBytecode(BytecodeGenerator state);

        Type getType();
    }

    public static class Builder implements SyntaxBuilder<FunctionValue> {

        private Optional<Symbol>         symbol;
        private Optional<List<Argument>> arguments;
        private Optional<Value>          body;
        private Optional<SourceLocation> sourceLocation;

        private Builder() {
            symbol = Optional.empty();
            arguments = Optional.empty();
            body = Optional.empty();
            sourceLocation = Optional.empty();
        }

        @Override
        public FunctionValue build() {
            return fn(
                require(sourceLocation, "Source location"),
                require(symbol, "Function symbol"),
                require(arguments, "Function arguments"),
                require(body, "Function body").collapse()
            );
        }

        public Builder withArguments(List<Argument> arguments) {
            this.arguments = Optional.of(arguments);
            return this;
        }

        public Builder withBody(Value body) {
            this.body = Optional.of(body);
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
    }

    private static class CurriedBody implements CurriedFunction {

        private final Value body;

        public CurriedBody(Value body) {
            this.body = body;
        }

        @Override
        public CodeBlock generateBytecode(BytecodeGenerator state) {
            return body.generateBytecode(state);
        }

        @Override
        public Type getType() {
            return body.getType();
        }
    }

    private static class CurriedLambda implements CurriedFunction {

        private final Argument        argument;
        private final CurriedFunction body;

        CurriedLambda(Argument argument, CurriedFunction body) {
            this.argument = argument;
            this.body = body;
        }

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
