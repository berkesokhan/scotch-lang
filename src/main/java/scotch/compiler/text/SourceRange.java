package scotch.compiler.text;

import static lombok.AccessLevel.PRIVATE;
import static scotch.compiler.text.SourcePoint.point;
import static scotch.util.StringUtil.quote;

import java.net.URI;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.ToString;
import me.qmx.jitescript.CodeBlock;
import org.objectweb.asm.tree.LabelNode;

@AllArgsConstructor(access = PRIVATE)
@ToString
public class SourceRange {

    public static final SourceRange NULL_SOURCE = source(URI.create("null://nowhere"), point(-1, -1, -1), point(-1, -1, -1));

    public static SourceRange source(URI source, SourcePoint start, SourcePoint end) {
        return new SourceRange(source, start, end);
    }

    public static SourceRange source(String source, SourcePoint start, SourcePoint end) {
        return source(URI.create(source), start, end);
    }

    private final URI         source;
    private final SourcePoint start;
    private final SourcePoint end;

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof SourceRange) {
            SourceRange other = (SourceRange) o;
            return this == NULL_SOURCE || other == NULL_SOURCE || (
                Objects.equals(source, other.source)
                    && Objects.equals(start, other.start)
                    && Objects.equals(end, other.end)
            );
        } else {
            return false;
        }
    }

    public SourceRange extend(SourceRange sourceRange) {
        if (this == NULL_SOURCE) {
            return sourceRange;
        } else if (sourceRange == NULL_SOURCE) {
            return this;
        } else {
            return source(source, start.min(sourceRange.start), end.max(sourceRange.end));
        }
    }

    public NamedSourcePoint getEnd() {
        return end.withSource(source);
    }

    public int getEndOffset() {
        return end.getOffset();
    }

    public URI getSource() {
        return source;
    }

    public String getPath() {
        return source.getPath();
    }

    public NamedSourcePoint getStart() {
        return start.withSource(source);
    }

    public int getStartOffset() {
        return start.getOffset();
    }

    public SourceRange getStartRange() {
        return new SourceRange(source, start, start);
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
                block.line(line, label);
            }
        }
    }

    public String prettyPrint() {
        return "[" + quote(getPath()) + " " + start.prettyPrint() + ", " + end.prettyPrint() + "]";
    }
}
