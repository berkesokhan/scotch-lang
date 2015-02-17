package scotch.compiler.syntax.scope;

import org.junit.Before;
import org.junit.Test;
import scotch.compiler.symbol.SymbolGenerator;

public class DefaultTypeScopeTest {

    private DefaultTypeScope scope;

    @Before
    public void setUp() {
        scope = new DefaultTypeScope(new SymbolGenerator());
    }

    @Test
    public void shouldFindImplementationByType() {

    }
}
