package scotch.compiler.util;

import java.util.Objects;
import org.apache.commons.lang.builder.EqualsBuilder;

public final class SourceCoordinate {

    public static final SourceCoordinate NULL_COORDINATE = coordinate(-1, -1, -1);

    public static SourceCoordinate coordinate(int offset, int line, int column) {
        return new SourceCoordinate(offset, line, column);
    }

    private final int offset;
    private final int line;
    private final int column;

    public SourceCoordinate(int offset, int line, int column) {
        this.offset = offset;
        this.line = line;
        this.column = column;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof SourceCoordinate) {
            SourceCoordinate other = (SourceCoordinate) o;
            return new EqualsBuilder()
                .append(getOffset(), other.getOffset())
                .append(getLine(), other.getLine())
                .append(getColumn(), other.getColumn())
                .isEquals();
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
        return Objects.hash(getOffset(), getLine(), getColumn());
    }

    public SourceCoordinate nextChar() {
        return coordinate(offset + 1, line, column + 1);
    }

    public SourceCoordinate nextLine() {
        return coordinate(offset + 1, line + 1, 1);
    }

    public SourceCoordinate nextTab() {
        return coordinate(offset + 1, line, column + 8);
    }

    @Override
    public String toString() {
        return "(" + getLine() + ", " + getColumn() + ")";
    }
}
