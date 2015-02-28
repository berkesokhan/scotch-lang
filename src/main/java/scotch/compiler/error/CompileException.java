package scotch.compiler.error;

import static java.lang.System.err;
import static java.util.stream.Collectors.joining;

import java.util.LinkedHashSet;
import java.util.List;

public class CompileException extends RuntimeException {

    private final LinkedHashSet<SyntaxError> errors;

    public CompileException(List<SyntaxError> errors) {
        this.errors = new LinkedHashSet<>(errors);
    }

    @Override
    public String getMessage() {
        return "Failed compilation:\n\n" + errors.stream()
            .map(SyntaxError::prettyPrint)
            .collect(joining("\n\n"));
    }

    public void printErrors() {
        err.println("Failed compilation:\n\n" + errors.stream()
            .map(error -> error.report("\t", 1))
            .collect(joining("\n\n")));
    }
}
