package scotch.compiler.parser;

import static java.util.Arrays.asList;
import static scotch.compiler.error.ParseError.parseError;

import scotch.compiler.error.CompileException;
import scotch.compiler.text.SourceLocation;

public class ParseException extends CompileException {

    public ParseException(String message, SourceLocation location) {
        super(asList(parseError(message, location)));
    }
}
