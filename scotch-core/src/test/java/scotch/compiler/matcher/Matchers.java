package scotch.compiler.matcher;

import org.hamcrest.Matcher;
import scotch.compiler.ast.Import;
import scotch.compiler.ast.Scope;

public final class Matchers {

    public static Matcher<Scope> hasForwardReferences(String... symbols) {
        return new ForwardReferenceMatcher(symbols);
    }

    public static Matcher<Scope> hasImports(Import... imports) {
        return new ImportMatcher(imports);
    }

    public static Matcher<Scope> hasReferences(String... symbols) {
        return new ReferenceMatcher(symbols);
    }

    private Matchers() {
        // intentionally empty
    }
}
