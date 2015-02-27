package scotch.compiler.syntax.scope;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static scotch.compiler.symbol.Symbol.symbol;
import static scotch.compiler.symbol.Symbol.unqualified;
import static scotch.compiler.syntax.scope.Scope.scope;
import static scotch.compiler.util.TestUtil.intType;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import scotch.compiler.symbol.Operator;
import scotch.compiler.symbol.util.SymbolGenerator;
import scotch.compiler.symbol.SymbolResolver;
import scotch.compiler.symbol.type.Types;

@RunWith(MockitoJUnitRunner.class)
public class ChildScopeTest {

    @Rule
    public final ExpectedException exception = none();
    @Mock
    private Scope parentScope;
    private Scope childScope;

    @Before
    public void setUp() {
        childScope = scope("scotch.test", parentScope, new DefaultTypeScope(new SymbolGenerator(), mock(SymbolResolver.class)));
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
    public void shouldNotDefineOperator() {
        exception.expect(IllegalStateException.class);
        childScope.defineOperator(unqualified("fn"), mock(Operator.class));
    }

    @Test
    public void shouldNotDelegateToParentWhenValueDefined() {
        childScope.defineValue(unqualified("x"), Types.t(2));
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
