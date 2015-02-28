package scotch.compiler.steps;

import static java.util.Arrays.asList;
import static scotch.compiler.error.ParseError.parseError;
import static scotch.compiler.syntax.value.Values.apply;
import static scotch.compiler.syntax.value.Values.unshuffled;
import static scotch.compiler.util.Either.left;
import static scotch.compiler.util.Either.right;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;
import scotch.compiler.error.SyntaxError;
import scotch.compiler.symbol.type.Type;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.syntax.value.Identifier;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.util.Either;

public class ValueShuffler {

    private final Function<Value, Value> parser;

    public ValueShuffler(Function<Value, Value> parser) {
        this.parser = parser;
    }

    public Either<SyntaxError, Value> shuffle(Scope scope, List<Value> message) {
        if (message.size() == 1) {
            Value value = parser.apply(message.get(0));
            return right(unshuffled(value.getSourceRange(), asList(value)));
        } else {
            try {
                return right(parser.apply(new Shuffler(scope, message).shuffleMessage()));
            } catch (ShuffleException exception) {
                return left(exception.syntaxError);
            }
        }
    }

    private static final class ShuffleException extends RuntimeException {

        private final SyntaxError syntaxError;

        private ShuffleException(SyntaxError syntaxError) {
            super(syntaxError.prettyPrint());
            this.syntaxError = syntaxError;
        }
    }

    private final class Shuffler {

        private final Scope       scope;
        private final List<Value> message;

        public Shuffler(Scope scope, List<Value> message) {
            this.scope = scope;
            this.message = message;
        }

        public Value shuffleMessage() {
            Deque<Value> input = new ArrayDeque<>(message);
            Deque<Either<OperatorPair<Identifier>, Value>> output = new ArrayDeque<>();
            Deque<OperatorPair<Identifier>> stack = new ArrayDeque<>();
            boolean expectsPrefix = isOperator(input.peek());
            while (!input.isEmpty()) {
                if (isOperator(input.peek())) {
                    OperatorPair<Identifier> o1 = getOperator(input.poll(), expectsPrefix);
                    while (!stack.isEmpty() && o1.isLessPrecedentThan(stack.peek())) {
                        output.push(left(stack.pop()));
                    }
                    stack.push(o1);
                    expectsPrefix = isOperator(input.peek());
                } else {
                    output.push(right(shuffleNext(input)));
                    while (expectsArgument(input)) {
                        output.push(right(apply(
                            output.pop().orElseGet(OperatorPair::getValue),
                            shuffleNext(input),
                            reserveType()
                        )));
                    }
                }
            }
            while (!stack.isEmpty()) {
                output.push(left(stack.pop()));
            }
            return shuffleMessageApply(output);
        }

        private boolean expectsArgument(Deque<Value> input) {
            return !input.isEmpty() && expectsArgument_(input);
        }

        private boolean expectsArgument_(Deque<Value> input) {
            return !isOperator(input.peek());
        }

        private OperatorPair<Identifier> getOperator(Value value, boolean expectsPrefix) {
            return value.asOperator(scope)
                .map(pair -> pair.into((identifier, operator) -> {
                    if (expectsPrefix && !operator.isPrefix()) {
                        throw new ShuffleException(parseError("Unexpected binary operator " + identifier.getSymbol(), identifier.getSourceRange()));
                    } else {
                        return new OperatorPair<>(operator, identifier);
                    }
                }))
                .orElseThrow(() -> new ShuffleException(parseError("Value " + value.prettyPrint() + " is not an operator", value.getSourceRange())));
        }

        private boolean isOperator(Value value) {
            return value.isOperator(scope);
        }

        private Type reserveType() {
            return scope.reserveType();
        }

        private Value shuffleMessageApply(Deque<Either<OperatorPair<Identifier>, Value>> message) {
            Deque<Value> stack = new ArrayDeque<>();
            while (!message.isEmpty()) {
                stack.push(message.pollLast().orElseGet(pair -> {
                    if (pair.isPrefix()) {
                        return apply(pair.getValue(), stack.pop(), reserveType());
                    } else {
                        Value right = stack.pop();
                        Value left = stack.pop();
                        return apply(apply(pair.getValue(), left, reserveType()), right, reserveType());
                    }
                }));
            }
            if (stack.size() > 1) {
                stack.push(apply(stack.pollLast(), stack.pollLast(), reserveType()));
            }
            return stack.pop();
        }

        private Value shuffleNext(Deque<Value> input) {
            return input.poll().destructure()
                .map(values -> shuffle(scope, values).orElseThrow(ShuffleException::new))
                .orElseGet(left -> left);
        }
    }
}
