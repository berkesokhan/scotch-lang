package scotch.compiler;

import static java.util.stream.Collectors.joining;

import java.util.List;
import scotch.compiler.syntax.SyntaxError;

public class CompileException extends RuntimeException {

    public CompileException(List<SyntaxError> errors) {
        super("Failed compilation:\n\t" + errors.stream().map(SyntaxError::prettyPrint).collect(joining("\n\t")));
    }
}
