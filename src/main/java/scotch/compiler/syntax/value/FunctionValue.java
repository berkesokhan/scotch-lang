package scotch.compiler.syntax.value;

import static java.util.Collections.reverse;
import static java.util.stream.Collectors.toList;
import static scotch.compiler.syntax.reference.DefinitionReference.scopeRef;
import static scotch.util.StringUtil.stringify;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import me.qmx.jitescript.CodeBlock;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.syntax.BytecodeGenerator;
import scotch.compiler.syntax.Scoped;
import scotch.compiler.syntax.SyntaxTreeParser;
import scotch.compiler.syntax.TypeChecker;
import scotch.compiler.syntax.definition.Definition;
import scotch.compiler.syntax.reference.ScopeReference;
import scotch.compiler.text.SourceRange;

public class FunctionValue extends Value implements Scoped {

    private final SourceRange    sourceRange;
    private final Symbol         symbol;
    private final List<Argument> arguments;
    private final Value          body;
    private final Optional<Type> type;

    FunctionValue(SourceRange sourceRange, Symbol symbol, List<Argument> arguments, Value body, Optional<Type> type) {
        this.sourceRange = sourceRange;
        this.symbol = symbol;
        this.arguments = ImmutableList.copyOf(arguments);
        this.body = body;
        this.type = type;
    }

    @Override
    public <T> T accept(ValueVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public Value accumulateDependencies(SyntaxTreeParser state) {
        return state.keep(withBody(body.accumulateDependencies(state)));
    }

    @Override
    public Value accumulateNames(SyntaxTreeParser state) {
        return state.scoped(this, () -> withArguments(arguments.stream()
            .map(argument -> (Argument) argument.accumulateNames(state))
            .collect(toList()))
            .withBody(body.accumulateNames(state)));
    }

    @Override
    public Value bindMethods(TypeChecker state) {
        return state.scoped(this, () -> withBody(body.bindMethods(state)));
    }

    @Override
    public Value checkTypes(TypeChecker state) {
        return state.enclose(this, () -> {
            List<Type> argumentTypes = arguments.stream()
                .map(Argument::getType)
                .collect(toList());
            arguments.stream()
                .map(Argument::getSymbol)
                .forEach(state::addLocal);
            argumentTypes.forEach(state::specialize);
            return withBody(body.checkTypes(state))
                .withArguments(arguments.stream()
                    .map(arg -> arg.withType(state.generate(arg.getType())))
                    .collect(toList()));
        });
    }

    @Override
    public Value bindTypes(TypeChecker state) {
        return withArguments(arguments.stream()
            .map(argument -> (Argument) argument.bindTypes(state))
            .collect(toList()))
            .withBody(body.bindTypes(state))
            .withType(state.generate(getType()));
    }

    @Override
    public Value defineOperators(SyntaxTreeParser state) {
        return state.scoped(this, () -> withBody(body.defineOperators(state)));
    }

    public Value curry() {
        return curry_(new ArrayDeque<>(arguments));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof FunctionValue) {
            FunctionValue other = (FunctionValue) o;
            return Objects.equals(sourceRange, other.sourceRange)
                && Objects.equals(symbol, other.symbol)
                && Objects.equals(arguments, other.arguments)
                && Objects.equals(body, other.body);
        } else {
            return false;
        }
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
        return scopeDef(this);
    }

    public ScopeReference getReference() {
        return scopeRef(symbol);
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
        return type.orElseGet(() -> {
            List<Argument> args = new ArrayList<>(arguments);
            reverse(args);
            return args.stream()
                .map(Argument::getType)
                .reduce(body.getType(), (result, arg) -> Type.fn(arg, result));
        });
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, arguments, body);
    }

    @Override
    public Value qualifyNames(SyntaxTreeParser state) {
        return state.scoped(this, () -> withBody(body.qualifyNames(state)));
    }

    @Override
    public Value parsePrecedence(SyntaxTreeParser state) {
        return state.scoped(this, () -> withBody(body.parsePrecedence(state)));
    }

    @Override
    public String toString() {
        return stringify(this) + "(" + arguments + " -> " + body + ")";
    }

    public FunctionValue withArguments(List<Argument> arguments) {
        return fn(sourceRange, symbol, arguments, body);
    }

    public FunctionValue withBody(Value body) {
        return fn(sourceRange, symbol, arguments, body);
    }

    @Override
    public FunctionValue withType(Type type) {
        return new FunctionValue(sourceRange, symbol, arguments, body, Optional.of(type));
    }

    private Value curry_(Deque<Argument> args) {
        if (args.isEmpty()) {
            return body;
        } else {
            return new LambdaValue(args.pop(), curry_(args));
        }
    }
}
