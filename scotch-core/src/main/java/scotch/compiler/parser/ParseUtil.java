package scotch.compiler.parser;

import java.util.Deque;

public final class ParseUtil {

    public static <T> boolean nextOperatorHasGreaterPrecedence(OperatorPair<T> current, Deque<OperatorPair<T>> stack) {
        return !stack.isEmpty() && nextOperatorHasGreaterPrecedence(current, stack.peek());
    }

    private static <T> boolean nextOperatorHasGreaterPrecedence(OperatorPair<T> current, OperatorPair<T> next) {
        return current.isLeftAssociative() && current.hasSamePrecedenceAs(next)
            || current.hasLessPrecedenceThan(next);
    }

    private ParseUtil() {
        // intentionally empty
    }
}
