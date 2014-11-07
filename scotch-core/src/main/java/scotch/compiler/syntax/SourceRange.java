package scotch.compiler.syntax;

import static scotch.compiler.util.TextUtil.quote;
import static scotch.compiler.util.TextUtil.stringify;

import java.util.Objects;

public class SourceRange {

    public static final SourceRange NULL_SOURCE = source("NULL", point(-1, -1, -1), point(-1, -1, -1));

    public static SourceRange source(String source, SourcePoint start, SourcePoint end) {
        return new SourceRange(source, start, end);
    }

    public static SourcePoint point(int offset, int line, int column) {
        return new SourcePoint(offset, line, column);
    }

    private final String      sourceName;
    private final SourcePoint start;
    private final SourcePoint end;

    private SourceRange(String sourceName, SourcePoint start, SourcePoint end) {
        this.sourceName = sourceName;
        this.start = start;
        this.end = end;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof SourceRange) {
            SourceRange other = (SourceRange) o;
            return this == NULL_SOURCE || other == NULL_SOURCE || (
                Objects.equals(sourceName, other.sourceName)
                    && Objects.equals(start, other.start)
                    && Objects.equals(end, other.end)
            );
        } else {
            return false;
        }
    }

    public NamedSourcePoint getStart() {
        return NamedSourcePoint.source(sourceName, start.getOffset(), start.getLine(), start.getColumn());
    }

    public String prettyPrint() {
        return "[" + quote(sourceName) + " " + start.prettyPrint() + ", " + end.prettyPrint() + "]";
    }

    @Override
    public String toString() {
        return stringify(this) + "(source=" + quote(sourceName) + ", start=" + start + ", end=" + end + ")";
    }
}
