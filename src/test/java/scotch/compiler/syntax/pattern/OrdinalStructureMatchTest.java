package scotch.compiler.syntax.pattern;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static scotch.compiler.util.TestUtil.capture;
import static scotch.compiler.util.TestUtil.ordinalField;
import static scotch.compiler.util.TestUtil.ordinalStruct;
import static scotch.symbol.type.Types.t;

import org.junit.Test;
import scotch.compiler.syntax.scope.Scope;
import scotch.symbol.type.Type;

public class OrdinalStructureMatchTest {

    @Test
    public void shouldBindTupleMatch() {
        Type t = t(0);
        PatternMatch struct = ordinalStruct("(,)", t, asList(ordinalField(t, capture("a", t(1))), ordinalField(t, capture("b", t(2)))));
        assertThat(struct.bind("#0", mock(Scope.class)), is(ordinalStruct("#0", "(,)", t, asList(
            ordinalField("#0", "_0", t, capture("#0#_0", "a", t(1))),
            ordinalField("#0", "_1", t, capture("#0#_1", "b", t(2)))
        ))));
    }
}
