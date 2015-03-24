package scotch.compiler.syntax.scope;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static scotch.compiler.syntax.scope.Scope.scope;
import static scotch.compiler.util.TestUtil.intType;
import static scotch.symbol.Symbol.symbol;
import static scotch.symbol.Symbol.unqualified;
import static scotch.symbol.type.Types.t;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import scotch.symbol.SymbolResolver;
import scotch.symbol.util.SymbolGenerator;

@RunWith(MockitoJUnitRunner.class)
public class ChildScopeTest {

    @Rule
    public final ExpectedException exception = none();
    @Mock
    private Scope parentScope;
    private Scope childScope;

    @Before
    public void setUp() {
        when(parentScope.reserveType()).thenReturn(t(20));
        SymbolResolver symbolResolver = mock(SymbolResolver.class);
        SymbolGenerator symbolGenerator = new SymbolGenerator();
        childScope = scope(parentScope, new DefaultTypeScope(symbolGenerator, symbolResolver), symbolResolver, symbolGenerator, "scotch.test");
    }

    @Test
    public void leavingScopeShouldGiveParent() {
        assertThat(childScope.leaveScope(), sameInstance(parentScope));
        assertThat(childScope.enterScope().leaveScope(), sameInstance(childScope));
    }

    @Test
    public void shouldDelegateToParentWhenQualifyingSymbolThisIsNotDefinedLocally() {
        childScope.qualify(unqualified("fn"));
        verify(parentScope).qualify(unqualified("fn"));
    }

    @Test
    public void shouldNotDelegateToParentWhenValueDefined() {
        childScope.defineValue(unqualified("x"), t(2));
        childScope.getValue(unqualified("x"));
        verify(parentScope, never()).getValue(unqualified("x"));
    }

    @Test
    public void shouldGetContextFromParent() {
        when(parentScope.getContext(intType())).thenReturn(ImmutableSet.of(symbol("scotch.data.num.Num")));
        assertThat(childScope.getContext(intType()), contains(
            symbol("scotch.data.num.Num")
        ));
    }
}
