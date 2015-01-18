package scotch.compiler.symbol;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.symbol.Symbol.qualified;

import org.junit.Test;

public class SymbolTest {

    @Test
    public void shouldParenthesizeAndPrefixNumericMemberNames() {
        assertThat(Symbol.fromString("scotch.test.(#1)").getCanonicalName(), is("scotch.test.(#1)"));
    }

    @Test
    public void shouldGiveBracesForListName() {
        assertThat(Symbol.fromString("scotch.test.[]").getCanonicalName(), is("scotch.test.[]"));
    }

    @Test
    public void shouldGiveTupleForTupleName() {
        assertThat(Symbol.fromString("scotch.test.(,,,)").getCanonicalName(), is("scotch.test.(,,,)"));
    }

    @Test
    public void shouldParenthesizeNameWithSymbolCharacters() {
        assertThat(qualified("scotch.test", "%%").getCanonicalName(), is("scotch.test.(%%)"));
        assertThat(qualified("scotch.test", "...").getCanonicalName(), is("scotch.test.(...)"));
    }

    @Test
    public void shouldGiveMultipleMemberNamesWithHashSeparator() {
        assertThat(Symbol.fromString("scotch.test.(main#fn#2)").getMemberNames(), contains("main", "fn", "2"));
        assertThat(Symbol.fromString("main#fn#2").getMemberNames(), contains("main", "fn", "2"));
    }

    @Test
    public void shouldPrefixNumbers() {
        assertThat(Symbol.fromString("1").getCanonicalName(), is("#1"));
    }

    @Test
    public void shouldPrefixMultipleMemberNames_whenStartingWithNumber() {
        assertThat(Symbol.fromString("1#test#fn").getCanonicalName(), is("#1#test#fn"));
    }
}
