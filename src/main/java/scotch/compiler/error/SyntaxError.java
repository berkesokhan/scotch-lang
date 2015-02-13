package scotch.compiler.error;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.google.common.collect.ImmutableList;

public abstract class SyntaxError {

    private static List<StackTraceElement> formatStackTrace() {
        List<StackTraceElement> stackTrace = new ArrayList<>(asList(currentThread().getStackTrace()));
        Iterator<StackTraceElement> iterator = stackTrace.iterator();
        while (iterator.hasNext()) {
            StackTraceElement element = iterator.next();
            if (element.getClassName().contains("Thread") || element.getClassName().contains("SyntaxError")) {
                iterator.remove();
            } else {
                break;
            }
        }
        return ImmutableList.copyOf(stackTrace);
    }

    private final List<StackTraceElement> stackTrace;

    public SyntaxError() {
        this.stackTrace = formatStackTrace();
    }

    @Override
    public abstract boolean equals(Object o);

    public List<StackTraceElement> getStackTrace() {
        return stackTrace;
    }

    @Override
    public abstract int hashCode();

    public abstract String prettyPrint();

    @Override
    public String toString() {
        return prettyPrint();
    }
}
