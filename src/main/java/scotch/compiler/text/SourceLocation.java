package scotch.compiler.text;

import static lombok.AccessLevel.PRIVATE;
import static scotch.compiler.text.SourcePoint.point;
import static scotch.compiler.text.TextUtil.repeat;

import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import lombok.AllArgsConstructor;
import me.qmx.jitescript.CodeBlock;
import org.objectweb.asm.tree.LabelNode;

@AllArgsConstructor(access = PRIVATE)
public class SourceLocation {

    public static final SourceLocation NULL_SOURCE = source(URI.create("null://nowhere"), point(-1, -1, -1), point(-1, -1, -1));

    public static SourceLocation extent(Collection<SourceLocation> locations) {
        Iterator<SourceLocation> iterator = locations.iterator();
        if (iterator.hasNext()) {
            SourceLocation location = iterator.next();
            while (iterator.hasNext()) {
                location = location.extend(iterator.next());
            }
            return location;
        } else {
            return NULL_SOURCE;
        }
    }

    public static SourceLocation source(String source, SourcePoint start, SourcePoint end) {
        return source(URI.create(source), start, end);
    }

    public static SourceLocation source(URI source, SourcePoint start, SourcePoint end) {
        return new SourceLocation(source, start, end);
    }

    private final URI         source;
    private final SourcePoint start;
    private final SourcePoint end;

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof SourceLocation) {
            SourceLocation other = (SourceLocation) o;
            return this == NULL_SOURCE || other == NULL_SOURCE || (
                Objects.equals(source, other.source)
                    && Objects.equals(start, other.start)
                    && Objects.equals(end, other.end)
            );
        } else {
            return false;
        }
    }

    public SourceLocation extend(SourceLocation sourceLocation) {
        if (this == NULL_SOURCE) {
            return sourceLocation;
        } else if (sourceLocation == NULL_SOURCE) {
            return this;
        } else {
            return source(source, start.min(sourceLocation.start), end.max(sourceLocation.end));
        }
    }

    public NamedSourcePoint getEnd() {
        return end.withSource(source);
    }

    public int getEndOffset() {
        return end.getOffset();
    }

    public SourceLocation getEndPoint() {
        return new SourceLocation(source, end, end);
    }

    public String getPath() {
        String path = source.getPath();
        if (path.isEmpty()) {
            return source.toString();
        } else {
            return path;
        }
    }

    public URI getSource() {
        return source;
    }

    public NamedSourcePoint getStart() {
        return start.withSource(source);
    }

    public int getStartOffset() {
        return start.getOffset();
    }

    public SourceLocation getStartPoint() {
        return new SourceLocation(source, start, start);
    }

    @Override
    public int hashCode() {
        return 79;
    }

    public void markLine(CodeBlock block) {
        if (this != NULL_SOURCE) {
            LabelNode label = new LabelNode();
            int line = start.getLine();
            if (line != -1) {
                block.label(label);
                block.line(line - 1, label);
            }
        }
    }

    public String prettyPrint() {
        return "[" + prettyPrint_() + "]";
    }

    public String report(String indent, int indentLevel) {
        return repeat(indent, indentLevel) + prettyPrint_();
    }

    @Override
    public String toString() {
        return prettyPrint();
    }

    private String prettyPrint_() {
        return getPath() + " " + start.prettyPrint() + ", " + end.prettyPrint();
    }
}
