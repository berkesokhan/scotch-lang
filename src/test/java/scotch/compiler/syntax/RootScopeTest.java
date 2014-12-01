package scotch.compiler.syntax;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static scotch.compiler.syntax.Scope.scope;
import static scotch.compiler.syntax.Symbol.qualified;
import static scotch.compiler.syntax.Symbol.unqualified;
import static scotch.compiler.syntax.Type.t;
import static scotch.compiler.util.TestUtil.moduleImport;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RootScopeTest {

    @Mock
    private SymbolResolver resolver;
    @Mock
    private TypeGenerator typeGenerator;
    private Scope          rootScope;
    private Scope          module1Scope;
    private Scope          module2Scope;

    @Test
    public void nothingShouldBeAnOperator() {
        assertThat(rootScope.isOperator_(qualified("scotch.module1", "fn")), is(false));
        assertThat(rootScope.isOperator_(unqualified("fn")), is(false));
    }

    @Before
    public void setUp() {
        rootScope = scope(typeGenerator, resolver);
        module1Scope = rootScope.enterScope("scotch.module1", emptyList());
        module2Scope = rootScope.enterScope("scotch.module2", asList(moduleImport("scotch.module1")));
    }

    @Test
    public void shouldDelegateQualifyingSymbolToSiblingModule() {
        module1Scope.defineValue(qualified("scotch.module1", "fn"), t(1));
        assertThat(module2Scope.qualify(unqualified("fn")), is(Optional.of(qualified("scotch.module1", "fn"))));
    }

    @Test
    public void shouldDelegateToResolver_whenQualifyingSymbolNotFoundInModules() {
        Symbol symbol = qualified("scotch.module1", "fn");
        when(resolver.isDefined(symbol)).thenReturn(true);
        rootScope.qualify(symbol);
        verify(resolver).isDefined(symbol);
    }

    @Test
    public void shouldDelegateToResolver_whenGettingEntryForQualifiedSymbolNotFoundInModules() {
        Symbol symbol = qualified("scotch.module1", "fn");
        when(resolver.isDefined(symbol)).thenReturn(true);
        when(resolver.getEntry(symbol)).thenReturn(mock(SymbolEntry.class));
        rootScope.getEntry(symbol);
        verify(resolver).getEntry(symbol);
    }

    @Test
    public void shouldLeavingChildScopeShouldGiveBackRootScope() {
        assertThat(rootScope.enterScope("scotch.module3", emptyList()).leaveScope(), sameInstance(rootScope));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrow_whenEnteringScopeWithoutModuleName() {
        rootScope.enterScope();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrow_whenGettingOperator() {
        rootScope.getOperator(qualified("scotch.module1", "+"));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrow_whenGettingValue() {
        rootScope.getValue(qualified("scotch.module1", "fn"));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrow_whenLeavingScope() {
        rootScope.leaveScope();
    }

    @Test(expected = SymbolNotFoundException.class)
    public void shouldThrow_whenQualifyingUndeclaredSymbol() {
        rootScope.qualify(qualified("scotch.module1", "fn"));
    }

    @Test(expected = SymbolNotFoundException.class)
    public void shouldThrow_whenQualifyingUnqualifiedSymbol() {
        rootScope.qualify(unqualified("fn"));
    }
}
