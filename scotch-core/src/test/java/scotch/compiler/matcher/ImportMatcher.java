package scotch.compiler.matcher;

import static java.util.Arrays.asList;

import java.util.List;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import scotch.compiler.ast.Import;
import scotch.compiler.ast.Scope;

public class ImportMatcher extends TypeSafeMatcher<Scope> {

    private final List<Import> imports;

    public ImportMatcher(Import... imports) {
        this.imports = asList(imports);
    }

    @Override
    public void describeTo(Description description) {
        description
            .appendText("scope to declare imports ")
            .appendValueList("[", ", ", "]", imports);
    }

    @Override
    protected void describeMismatchSafely(Scope item, Description mismatchDescription) {
        mismatchDescription
            .appendText("got ")
            .appendValueList("[", ", ", "]", item.getImports());
    }

    @Override
    protected boolean matchesSafely(Scope item) {
        return item.getImports().containsAll(imports);
    }
}
