package scotch.compiler.parser;

import static java.util.Arrays.asList;
import static scotch.compiler.parser.ParseError.parseError;
import static scotch.compiler.syntax.value.Value.apply;
import static scotch.compiler.syntax.value.Value.unshuffled;
import static scotch.data.either.Either.left;
import static scotch.data.either.Either.right;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;
import scotch.compiler.error.SyntaxError;
import scotch.compiler.symbol.Operator;
import scotch.compiler.symbol.Type;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.syntax.value.CaptureMatch;
import scotch.compiler.syntax.value.Identifier;
import scotch.compiler.syntax.value.PatternMatch;
import scotch.compiler.syntax.value.PatternMatch.PatternMatchVisitor;
import scotch.compiler.syntax.value.UnshuffledValue;
import scotch.compiler.syntax.value.Value;
import scotch.compiler.syntax.value.Value.ValueVisitor;
import scotch.data.either.Either;
import scotch.data.either.Either.EitherVisitor;

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
                            output.pop().getRightOr(OperatorPair::getValue),
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

        private boolean expectsArgument(Deque input) {
            return !input.isEmpty() && expectsArgument_(input);
        }

        private boolean expectsArgument_(Deque input) {
            if (input.peek() instanceof PatternMatch) {
                return !isOperator((PatternMatch) input.peek());
            } else {
                return !isOperator((Value) input.peek());
            }
        }

        private OperatorPair<Identifier> getOperator(Value value, boolean expectsPrefix) {
            return value.accept(new ValueVisitor<OperatorPair<Identifier>>() {
                @Override
                public OperatorPair<Identifier> visit(Identifier identifier) {
                    Operator operator = scope.qualify(identifier.getSymbol())
                        .map(scope::getOperator)
                        .orElseThrow(() -> new ShuffleException(parseError("Symbol is not an operator: " + identifier.getSymbol(), identifier.getSourceRange())));
                    if (expectsPrefix && !operator.isPrefix()) {
                        throw new ShuffleException(parseError("Unexpected binary operator " + identifier.getSymbol(), identifier.getSourceRange()));
                    }
                    return new OperatorPair<>(operator, identifier);
                }
            });
        }

        private boolean isOperator(PatternMatch match) {
            return match.accept(new PatternMatchVisitor<Boolean>() {
                @Override
                public Boolean visit(CaptureMatch match) {
                    return scope.isOperator(match.getSymbol());
                }

                @Override
                public Boolean visitOtherwise(PatternMatch match) {
                    return false;
                }
            });
        }

        private boolean isOperator(Value value) {
            return value.accept(new ValueVisitor<Boolean>() {
                @Override
                public Boolean visit(Identifier identifier) {
                    return scope.isOperator(identifier.getSymbol());
                }

                @Override
                public Boolean visitOtherwise(Value value) {
                    return false;
                }
            });
        }

        private Type reserveType() {
            return scope.reserveType();
        }

        private Value shuffleMessageApply(Deque<Either<OperatorPair<Identifier>, Value>> message) {
            Deque<Value> stack = new ArrayDeque<>();
            while (!message.isEmpty()) {
                stack.push(message.pollLast().getRightOr(pair -> {
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
            return input.poll().accept(new ValueVisitor<Value>() {
                @Override
                public Value visit(UnshuffledValue value) {
                    return shuffle(scope, value.getValues()).accept(new EitherVisitor<SyntaxError, Value, Value>() {
                        @Override
                        public Value visitLeft(SyntaxError left) {
                            throw new ShuffleException(left);
                        }

                        @Override
                        public Value visitRight(Value right) {
                            return right;
                        }
                    });
                }

                @Override
                public Value visitOtherwise(Value value) {
                    return value;
                }
            });
        }
    }
}
