package scotch.compiler.parser;

import static java.util.Collections.reverse;
import static scotch.compiler.syntax.SyntaxError.parseError;
import static scotch.data.either.Either.left;
import static scotch.data.either.Either.right;
import static scotch.util.StringUtil.quote;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import scotch.compiler.symbol.Operator;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.syntax.Definition.UnshuffledPattern;
import scotch.compiler.syntax.PatternMatch;
import scotch.compiler.syntax.PatternMatch.CaptureMatch;
import scotch.compiler.syntax.PatternMatch.PatternMatchVisitor;
import scotch.compiler.syntax.Scope;
import scotch.compiler.syntax.SyntaxError;
import scotch.data.either.Either;
import scotch.data.either.Either.EitherVisitor;

public class PatternShuffler {

    private static ShuffleResult error(SyntaxError error) {
        return new ShuffleResult() {
            @Override
            public <T> T accept(ResultVisitor<T> visitor) {
                return visitor.error(error);
            }
        };
    }

    private static ShuffleResult success(Symbol symbol, List<PatternMatch> matches) {
        return new ShuffleResult() {
            @Override
            public <T> T accept(ResultVisitor<T> visitor) {
                return visitor.success(symbol, matches);
            }
        };
    }

    public ShuffleResult shuffle(Scope scope, UnshuffledPattern pattern) {
        return new Shuffler(scope, pattern).splitPattern();
    }

    public interface ResultVisitor<T> {

        T error(SyntaxError error);

        T success(Symbol symbol, List<PatternMatch> matches);
    }

    private static final class ShuffleException extends RuntimeException {

        private final SyntaxError syntaxError;

        private ShuffleException(SyntaxError syntaxError) {
            super(syntaxError.prettyPrint());
            this.syntaxError = syntaxError;
        }
    }

    public static abstract class ShuffleResult {

        public abstract <T> T accept(ResultVisitor<T> visitor);
    }

    private final class Shuffler {

        private final Scope             scope;
        private final UnshuffledPattern pattern;

        private Shuffler(Scope scope, UnshuffledPattern pattern) {
            this.scope = scope;
            this.pattern = pattern;
        }

        public ShuffleResult splitPattern() {
            try {
                List<PatternMatch> matches = shufflePattern(pattern.getMatches());
                return matches.remove(0).accept(new PatternMatchVisitor<ShuffleResult>() {
                    @Override
                    public ShuffleResult visit(CaptureMatch match) {
                        return success(scope.qualifyCurrent(match.getSymbol()), matches);
                    }

                    @Override
                    public ShuffleResult visitOtherwise(PatternMatch match) {
                        return error(parseError("Illegal start of pattern", match.getSourceRange()));
                    }
                });
            } catch (ShuffleException exception) {
                return error(exception.syntaxError);
            }
        }

        private boolean expectsArgument(Deque<PatternMatch> input) {
            return !input.isEmpty() && !isOperator(input.peek());
        }

        private OperatorPair<CaptureMatch> getOperator(PatternMatch match, boolean expectsPrefix) {
            return match.accept(new PatternMatchVisitor<OperatorPair<CaptureMatch>>() {
                @Override
                public OperatorPair<CaptureMatch> visit(CaptureMatch match) {
                    Operator operator = scope.qualify(match.getSymbol())
                        .map(scope::getOperator)
                        .orElseThrow(() -> new ShuffleException(parseError("Symbol " + match.getSymbol().quote() + " is not an operator", match.getSourceRange())));
                    if (expectsPrefix && !operator.isPrefix()) {
                        throw new ShuffleException(parseError("Unexpected binary operator " + quote(match.getSymbol()), match.getSourceRange()));
                    }
                    return new OperatorPair<>(operator, match);
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

        private List<PatternMatch> shufflePattern(List<PatternMatch> matches) {
            Deque<PatternMatch> input = new ArrayDeque<>(matches);
            Deque<Either<OperatorPair<CaptureMatch>, PatternMatch>> output = new ArrayDeque<>();
            Deque<OperatorPair<CaptureMatch>> stack = new ArrayDeque<>();
            boolean expectsPrefix = isOperator(input.peek());
            while (!input.isEmpty()) {
                if (isOperator(input.peek())) {
                    OperatorPair<CaptureMatch> o1 = getOperator(input.poll(), expectsPrefix);
                    while (!stack.isEmpty() && o1.isLessPrecedentThan(stack.peek())) {
                        output.push(left(stack.pop()));
                    }
                    stack.push(o1);
                    expectsPrefix = isOperator(input.peek());
                } else {
                    output.push(right(input.poll()));
                    while (expectsArgument(input)) {
                        output.push(right(input.poll()));
                    }
                }
            }
            while (!stack.isEmpty()) {
                output.push(left(stack.pop()));
            }
            return shufflePatternApply(output);
        }

        private List<PatternMatch> shufflePatternApply(Deque<Either<OperatorPair<CaptureMatch>, PatternMatch>> input) {
            Deque<PatternMatch> output = new ArrayDeque<>();
            while (!input.isEmpty()) {
                input.pollLast().accept(new EitherVisitor<OperatorPair<CaptureMatch>, PatternMatch, Void>() {
                    @Override
                    public Void visitLeft(OperatorPair<CaptureMatch> left) {
                        if (left.isPrefix()) {
                            PatternMatch head = output.pop();
                            output.push(left.getValue());
                            output.push(head);
                        } else {
                            PatternMatch l = output.pop();
                            PatternMatch r = output.pop();
                            output.push(left.getValue());
                            output.push(r);
                            output.push(l);
                        }
                        return null;
                    }

                    @Override
                    public Void visitRight(PatternMatch right) {
                        output.push(right);
                        return null;
                    }
                });
            }
            List<PatternMatch> matches = new ArrayList<>(output);
            reverse(matches);
            return matches;
        }
    }
}
