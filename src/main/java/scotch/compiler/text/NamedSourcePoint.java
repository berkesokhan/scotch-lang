package scotch.compiler.text;

import static scotch.compiler.text.SourcePoint.point;
import static scotch.util.StringUtil.quote;

import java.net.URI;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = false)
@ToString
public class NamedSourcePoint {

    public static NamedSourcePoint source(URI source, int offset, int line, int column) {
        return new NamedSourcePoint(source, offset, line, column);
    }

    private final URI source;
    private final int offset;
    private final int line;
    private final int column;

    private NamedSourcePoint(URI source, int offset, int line, int column) {
        this.source = source;
        this.offset = offset;
        this.line = line;
        this.column = column;
    }

    public int getColumn() {
        return column;
    }

    public String prettyPrint() {
        return "[" + quote(source) + " (" + line + ", " + column + ")]";
    }

    public SourceLocation to(NamedSourcePoint end) {
        if (!isSameSourceAs(end)) {
            throw new IllegalArgumentException("Source location covers two sources: " + quote(source) + " and " + quote(end.source));
        }
        return SourceLocation.source(source, toPoint(), end.toPoint());
    }

    private boolean isSameSourceAs(NamedSourcePoint end) {
        return Objects.equals(source, end.source);
    }

    private SourcePoint toPoint() {
        return point(offset, line, column);
    }
}
