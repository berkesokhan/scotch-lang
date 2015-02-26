package scotch.compiler.scanner;

import static java.lang.String.join;
import static java.lang.System.lineSeparator;

import java.net.URI;
import scotch.compiler.text.NamedSourcePoint;

public interface Scanner {

    static Scanner forString(URI source, String... data) {
        return new LayoutScanner(new DefaultScanner(source, (join(lineSeparator(), data) + lineSeparator()).toCharArray()));
    }

    NamedSourcePoint getPosition();

    URI getSource();

    Token nextToken();
}
