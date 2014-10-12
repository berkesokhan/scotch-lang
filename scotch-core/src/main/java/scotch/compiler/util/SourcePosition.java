package scotch.compiler.util;

import static scotch.compiler.util.TextUtil.quote;

public final class SourcePosition {

    public static SourcePosition position(String source, SourceCoordinate coordinate) {
        return new SourcePosition(source, coordinate);
    }

    private final String           source;
    private final SourceCoordinate coordinate;

    public SourcePosition(String source, SourceCoordinate coordinate) {
        this.source = source;
        this.coordinate = coordinate;
    }

    public SourceCoordinate getCoordinate() {
        return coordinate;
    }

    public String getSource() {
        return source;
    }

    @Override
    public String toString() {
        return "[" + quote(getSource()) + " " + getCoordinate() + "]";
    }
}
