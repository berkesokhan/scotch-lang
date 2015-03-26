package scotch.symbol;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.symbol.Symbol.qualified;
import static scotch.symbol.Symbol.symbol;

import org.junit.Test;

public class SymbolTest {

    @Test
    public void shouldParenthesizeAndPrefixNumericMemberNames() {
        assertThat(symbol("scotch.test.(#1)").getCanonicalName(), is("scotch.test.(#1)"));
    }

    @Test
    public void shouldGiveBracesForListName() {
        assertThat(symbol("scotch.test.[]").getCanonicalName(), is("scotch.test.[]"));
    }

    @Test
    public void shouldGiveTupleForTupleName() {
        assertThat(symbol("scotch.test.(,,,)").getCanonicalName(), is("scotch.test.(,,,)"));
    }

    @Test
    public void shouldParenthesizeNameWithSymbolCharacters() {
        assertThat(qualified("scotch.test", "%%").getCanonicalName(), is("scotch.test.(%%)"));
        assertThat(qualified("scotch.test", "...").getCanonicalName(), is("scotch.test.(...)"));
    }

    @Test
    public void shouldGiveMultipleMemberNamesWithHashSeparator() {
        assertThat(symbol("scotch.test.(main#fn#2)").getMemberNames(), contains("main", "fn", "2"));
        assertThat(symbol("main#fn#2").getMemberNames(), contains("main", "fn", "2"));
    }

    @Test
    public void shouldPrefixNumbers() {
        assertThat(symbol("1").getCanonicalName(), is("#1"));
    }

    @Test
    public void shouldPrefixMultipleMemberNames_whenStartingWithNumber() {
        assertThat(symbol("1#test#fn").getCanonicalName(), is("#1#test#fn"));
    }

    @Test
    public void shouldPrefixNumericNameWithAlphaSuffix() {
        assertThat(symbol("#0i").getCanonicalName(), is("#0i"));
    }
}
