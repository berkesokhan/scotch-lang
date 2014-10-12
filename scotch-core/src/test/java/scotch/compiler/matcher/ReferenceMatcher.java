package scotch.compiler.matcher;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import java.util.List;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import scotch.compiler.ast.Scope;
import scotch.compiler.ast.Symbol;

public class ReferenceMatcher extends TypeSafeMatcher<Scope> {

    private final List<String> expectedSymbols;

    public ReferenceMatcher(String... symbols) {
        expectedSymbols = asList(symbols);
    }

    @Override
    public void describeTo(Description description) {
        description
            .appendText("to reference symbols ")
            .appendValueList("[", ", ", "]", expectedSymbols);
    }

    @Override
    protected void describeMismatchSafely(Scope item, Description mismatchDescription) {
        mismatchDescription
            .appendText("scope references ")
            .appendValueList("[", ", ", "]", item.getSymbols().stream().map(Symbol::getName).collect(toList()));
    }

    @Override
    protected boolean matchesSafely(Scope item) {
        return item.getSymbols().stream().allMatch(symbol -> expectedSymbols.contains(symbol.getName()));
    }
}
