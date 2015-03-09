package scotch.compiler.steps;

import static java.util.Collections.reverse;
import static scotch.compiler.error.ParseError.parseError;
import static scotch.compiler.util.Either.left;
import static scotch.compiler.util.Either.right;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import com.google.common.collect.ImmutableList;
import scotch.compiler.error.SyntaxError;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.syntax.definition.UnshuffledDefinition;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.syntax.pattern.CaptureMatch;
import scotch.compiler.syntax.pattern.PatternMatch;
import scotch.compiler.util.Either;

public class PatternShuffler {

    private static Either<SyntaxError, ShuffledPattern> success(Symbol symbol, List<PatternMatch> matches) {
        return right(new ShuffledPattern(symbol, matches));
    }

    public Either<SyntaxError, ShuffledPattern> shuffle(Scope scope, List<String> memberNames, UnshuffledDefinition pattern) {
        return new Shuffler(scope, memberNames, pattern).splitPattern();
    }

    private static final class ShuffleException extends RuntimeException {

        private final SyntaxError syntaxError;

        private ShuffleException(SyntaxError syntaxError) {
            super(syntaxError.prettyPrint());
            this.syntaxError = syntaxError;
        }
    }

    private final class Shuffler {

        private final Scope                scope;
        private final List<String>         memberNames;
        private final UnshuffledDefinition pattern;

        private Shuffler(Scope scope, List<String> memberNames, UnshuffledDefinition pattern) {
            this.scope = scope;
            this.memberNames = ImmutableList.copyOf(memberNames);
            this.pattern = pattern;
        }

        public Either<SyntaxError, ShuffledPattern> splitPattern() {
            try {
                List<PatternMatch> matches = shufflePattern(pattern.getMatches());
                return matches.remove(0).asCapture()
                    .map(match -> success(scope.qualifyCurrent(match.getSymbol()).nest(memberNames), matches))
                    .orElseGet(match -> left(parseError("Illegal start of pattern", match.getSourceRange())));
            } catch (ShuffleException exception) {
                return left(exception.syntaxError);
            }
        }

        private boolean expectsArgument(Deque<PatternMatch> input) {
            return !input.isEmpty() && !isOperator(input.peek());
        }

        private OperatorPair<CaptureMatch> getOperator(PatternMatch match, boolean expectsPrefix) {
            return match.asOperator(scope)
                .map(pair -> pair.into((capture, operator) -> {
                    if (expectsPrefix && !operator.isPrefix()) {
                        throw new ShuffleException(parseError("Unexpected binary operator " + capture.getSymbol(), capture.getSourceRange()));
                    } else {
                        return new OperatorPair<>(operator, capture);
                    }
                }))
                .orElseThrow(() -> new ShuffleException(parseError("Match " + match.prettyPrint() + " is not an operator", match.getSourceRange())));
        }

        private boolean isOperator(PatternMatch match) {
            return match.isOperator(scope);
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
                input.pollLast()
                    .map(right -> {
                        output.push(right);
                        return null;
                    })
                    .orElseGet(left -> {
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
                    });
            }
            List<PatternMatch> matches = new ArrayList<>(output);
            reverse(matches);
            return matches;
        }
    }
}
