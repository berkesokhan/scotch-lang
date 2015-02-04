package scotch.compiler.syntax.scope;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static scotch.compiler.symbol.Symbol.qualified;
import static scotch.compiler.symbol.Symbol.unqualified;
import static scotch.compiler.syntax.scope.Scope.scope;
import static scotch.compiler.util.TestUtil.moduleImport;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.SymbolEntry;
import scotch.compiler.symbol.SymbolGenerator;
import scotch.compiler.symbol.SymbolResolver;
import scotch.compiler.symbol.exception.SymbolNotFoundException;
import scotch.compiler.symbol.type.Types;

@RunWith(MockitoJUnitRunner.class)
public class RootScopeTest {

    @Mock
    private SymbolResolver  resolver;
    @Mock
    private SymbolGenerator symbolGenerator;
    private Scope           rootScope;
    private Scope           module1Scope;
    private Scope           module2Scope;

    @Before
    public void setUp() {
        when(resolver.getEntry(any(Symbol.class))).thenReturn(Optional.empty());
        rootScope = scope(symbolGenerator, resolver);
        module1Scope = rootScope.enterScope("scotch.module1", emptyList());
        module2Scope = rootScope.enterScope("scotch.module2", asList(moduleImport("scotch.module1")));
    }

    @Test
    public void nothingShouldBeAnOperator() {
        assertThat(rootScope.isOperator_(qualified("scotch.module1", "fn")), is(false));
        assertThat(rootScope.isOperator_(unqualified("fn")), is(false));
    }

    @Test
    public void shouldDelegateQualifyingSymbolToSiblingModule() {
        module1Scope.defineValue(qualified("scotch.module1", "fn"), Types.t(1));
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
        when(resolver.getEntry(symbol)).thenReturn(Optional.of(mock(SymbolEntry.class)));
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
