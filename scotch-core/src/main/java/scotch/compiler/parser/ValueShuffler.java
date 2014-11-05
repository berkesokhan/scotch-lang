package scotch.compiler.parser;

import static java.util.Arrays.asList;
import static scotch.compiler.syntax.Value.apply;
import static scotch.compiler.syntax.Value.message;
import static scotch.lang.Either.left;
import static scotch.lang.Either.right;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;
import scotch.compiler.ParseException;
import scotch.compiler.syntax.Operator;
import scotch.compiler.syntax.PatternMatch;
import scotch.compiler.syntax.PatternMatch.CaptureMatch;
import scotch.compiler.syntax.PatternMatch.PatternMatchVisitor;
import scotch.compiler.syntax.Type;
import scotch.compiler.syntax.Value;
import scotch.compiler.syntax.Value.Identifier;
import scotch.compiler.syntax.Value.Message;
import scotch.compiler.syntax.Value.ValueVisitor;
import scotch.lang.Either;

public class ValueShuffler {

    private final ScopeBuilder           scope;
    private final Function<Value, Value> parser;

    public ValueShuffler(ScopeBuilder scope, Function<Value, Value> parser) {
        this.scope = scope;
        this.parser = parser;
    }

    public Value shuffle(List<Value> message) {
        if (message.size() == 1) {
            return message(asList(parser.apply(message.get(0))));
        } else {
            return parser.apply(shuffleMessage_(message));
        }
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
                Operator operator = scope.qualify(identifier.getSymbol()).map(scope::getOperator).get(); // TODO
                if (expectsPrefix && !operator.isPrefix()) {
                    throw new ParseException("Unexpected binary operator " + identifier.getSymbol());
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

    private Value shuffleMessage_(List<Value> message) {
        Deque<Value> input = new ArrayDeque<>(message);
        Deque<Either<OperatorPair<Identifier>, Value>> output = new ArrayDeque<>();
        Deque<OperatorPair<Identifier>> stack = new ArrayDeque<>();
        boolean expectsPrefix = isOperator(input.peek());
        while (!input.isEmpty()) {
            if (isOperator(input.peek())) {
                OperatorPair<Identifier> o1 = getOperator(input.poll(), expectsPrefix);
                while (ParseUtil.nextOperatorHasGreaterPrecedence(o1, stack)) {
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

    private Value shuffleNext(Deque<Value> input) {
        return input.poll().accept(new ValueVisitor<Value>() {
            @Override
            public Value visit(Message message) {
                return shuffle(message.getMembers());
            }

            @Override
            public Value visitOtherwise(Value value) {
                return value;
            }
        });
    }
}
