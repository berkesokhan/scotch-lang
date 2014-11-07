package scotch.compiler.syntax;

import static scotch.compiler.util.TextUtil.stringify;

import java.util.Objects;

public class SourcePoint {

    private final int offset;
    private final int line;
    private final int column;

    SourcePoint(int offset, int line, int column) {
        this.offset = offset;
        this.line = line;
        this.column = column;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof SourcePoint) {
            SourcePoint other = (SourcePoint) o;
            return offset == other.offset
                && line == other.line
                && column == other.column;
        } else {
            return false;
        }
    }

    public int getColumn() {
        return column;
    }

    public int getLine() {
        return line;
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public int hashCode() {
        return Objects.hash(offset, line, column);
    }

    public SourcePoint nextChar() {
        return new SourcePoint(offset + 1, line, column + 1);
    }

    public SourcePoint nextLine() {
        return new SourcePoint(offset + 1, line + 1, 1);
    }

    public SourcePoint nextTab() {
        return new SourcePoint(offset + 1, line, column + 8);
    }

    public String prettyPrint() {
        return "(" + line + ", " + column + ")";
    }

    @Override
    public String toString() {
        return stringify(this) + "(offset=" + offset + ", line=" + line + ", column=" + column + ")";
    }

    public NamedSourcePoint withSourceName(String sourceName) {
        return NamedSourcePoint.source(sourceName, offset, line, column);
    }
}
