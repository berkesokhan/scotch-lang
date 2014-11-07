package scotch.compiler.util;

import static scotch.compiler.util.SourceCoordinate.NULL_COORDINATE;
import static scotch.compiler.util.TextUtil.quote;

import java.util.Objects;
import org.apache.commons.lang.builder.EqualsBuilder;

public final class SourceRange {

    public static final SourceRange NULL_SOURCE = range("unknown", NULL_COORDINATE, NULL_COORDINATE);

    public static SourceRange range(String source, SourceCoordinate start, SourceCoordinate end) {
        return new SourceRange(source, start, end);
    }

    private final String           source;
    private final SourceCoordinate start;
    private final SourceCoordinate end;

    public SourceRange(String source, SourceCoordinate start, SourceCoordinate end) {
        this.source = source;
        this.start = start;
        this.end = end;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this || this == NULL_SOURCE || o == NULL_SOURCE) {
            return true;
        } else if (o instanceof SourceRange) {
            SourceRange other = (SourceRange) o;
            return new EqualsBuilder()
                .append(getSource(), other.getSource())
                .append(getStart(), other.getStart())
                .append(getEnd(), other.getEnd())
                .isEquals();
        } else {
            return false;
        }
    }

    public SourceCoordinate getEnd() {
        return end;
    }

    public String getSource() {
        return source;
    }

    public SourceCoordinate getStart() {
        return start;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSource(), getStart(), getEnd());
    }

    @Override
    public String toString() {
        int length = getEnd().getOffset() - getStart().getOffset();
        return "[" + quote(getSource()) + " " + getStart() + ", " + getEnd() + "; length " + length + "]";
    }

    public static final class RangeBuilder {

        private String           source;
        private SourceCoordinate start;
        private SourceCoordinate end;

        public RangeBuilder setEnd(SourceCoordinate end) {
            this.end = end;
            return this;
        }

        public RangeBuilder setSource(String source) {
            this.source = source;
            return this;
        }

        public RangeBuilder setStart(SourceCoordinate start) {
            this.start = start;
            return this;
        }

        public SourceRange toPosition() {
            return new SourceRange(source, start, end);
        }
    }
}
