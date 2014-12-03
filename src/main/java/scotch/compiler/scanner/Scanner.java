package scotch.compiler.scanner;

import static java.lang.String.join;
import static java.lang.System.lineSeparator;

import scotch.compiler.text.NamedSourcePoint;

public interface Scanner {

    static Scanner forString(String source, String... data) {
        return new LayoutScanner(new DefaultScanner(source, (join(lineSeparator(), data) + lineSeparator()).toCharArray()));
    }

    NamedSourcePoint getPosition();

    String getSourceName();

    Token nextToken();
}
