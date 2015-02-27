package scotch.compiler.error;

import static java.lang.System.err;
import static java.util.stream.Collectors.joining;

import java.util.List;

public class CompileException extends RuntimeException {

    private final String message;

    public CompileException(List<SyntaxError> errors) {
        message = "Failed compilation:\n\t" + errors.stream().map(SyntaxError::prettyPrint).collect(joining("\n\t"));
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void printErrors() {
        err.println(message);
    }
}
