package scotch.compiler.syntax;

import static scotch.compiler.syntax.SourceRange.point;
import static scotch.compiler.util.TextUtil.quote;
import static scotch.compiler.util.TextUtil.stringify;

import java.util.Objects;

public class NamedSourcePoint {

    public static NamedSourcePoint source(String sourceName, int offset, int line, int column) {
        return new NamedSourcePoint(sourceName, offset, line, column);
    }

    private final String sourceName;
    private final int    offset;
    private final int    line;
    private final int    column;

    private NamedSourcePoint(String sourceName, int offset, int line, int column) {
        this.sourceName = sourceName;
        this.offset = offset;
        this.line = line;
        this.column = column;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof NamedSourcePoint) {
            NamedSourcePoint other = (NamedSourcePoint) o;
            return Objects.equals(sourceName, other.sourceName)
                && offset == other.offset
                && line == other.line
                && column == other.column;
        } else {
            return false;
        }
    }

    public int getColumn() {
        return column;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceName, offset, line, column);
    }

    public String prettyPrint() {
        return "[" + quote(sourceName) + " (" + line + ", " + column + ")]";
    }

    public SourceRange to(NamedSourcePoint end) {
        if (!isSameSourceAs(end)) {
            throw new IllegalArgumentException("Source range covers two sources: " + quote(sourceName) + " and " + quote(end.sourceName));
        }
        return SourceRange.source(sourceName, toPoint(), end.toPoint());
    }

    @Override
    public String toString() {
        return stringify(this) + "(source=" + quote(sourceName) + ", line=" + line + ", column=" + column + ", offset=" + offset + ")";
    }

    private boolean isSameSourceAs(NamedSourcePoint end) {
        return Objects.equals(sourceName, end.sourceName);
    }

    private SourcePoint toPoint() {
        return point(offset, line, column);
    }
}
