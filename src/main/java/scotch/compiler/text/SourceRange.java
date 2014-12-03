package scotch.compiler.text;

import static scotch.compiler.text.SourcePoint.point;
import static scotch.util.StringUtil.quote;
import static scotch.util.StringUtil.stringify;

import java.util.Objects;
import me.qmx.jitescript.CodeBlock;
import org.objectweb.asm.tree.LabelNode;

public class SourceRange {

    public static final SourceRange NULL_SOURCE = source("NULL", point(-1, -1, -1), point(-1, -1, -1));

    public static SourceRange source(String sourceName, SourcePoint start, SourcePoint end) {
        return new SourceRange(sourceName, start, end);
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

    public SourceRange extend(SourceRange sourceRange) {
        if (this == NULL_SOURCE) {
            return sourceRange;
        } else if (sourceRange == NULL_SOURCE) {
            return this;
        } else {
            return source(sourceName, start.min(sourceRange.start), end.max(sourceRange.end));
        }
    }

    public NamedSourcePoint getEnd() {
        return end.withSourceName(sourceName);
    }

    public String getSourceName() {
        return sourceName;
    }

    public NamedSourcePoint getStart() {
        return start.withSourceName(sourceName);
    }

    public void markLine(CodeBlock codeBlock) {
        LabelNode label = new LabelNode();
        codeBlock.label(label);
        codeBlock.line(start.getLine(), label);
    }

    public String prettyPrint() {
        return "[" + quote(sourceName) + " " + start.prettyPrint() + ", " + end.prettyPrint() + "]";
    }

    @Override
    public String toString() {
        return stringify(this) + "(source=" + quote(sourceName) + ", start=" + start + ", end=" + end + ")";
    }
}
