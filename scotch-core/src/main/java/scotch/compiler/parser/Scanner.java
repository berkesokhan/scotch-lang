package scotch.compiler.parser;

import static java.lang.String.join;
import static java.lang.System.lineSeparator;

import scotch.compiler.util.SourcePosition;

public interface Scanner {

    static Scanner forString(String source, String... data) {
        return new LayoutScanner(new DefaultScanner(source, (join(lineSeparator(), data) + lineSeparator()).toCharArray()));
    }

    SourcePosition getPosition();

    String getSource();

    Token nextToken();
}
