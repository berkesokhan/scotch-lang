package scotch.compiler.ast;

import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static scotch.compiler.ast.Scope.scope;
import static scotch.compiler.ast.Symbol.qualified;
import static scotch.compiler.ast.Symbol.unqualified;
import static scotch.compiler.ast.Type.t;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ChildScopeTest {

    @Rule
    public final ExpectedException exception = none();
    @Mock
    private Scope parentScope;
    private Scope childScope;

    @Test
    public void leavingScopeShouldGiveParent() {
        assertThat(childScope.leaveScope(), sameInstance(parentScope));
        assertThat(childScope.enterScope().leaveScope(), sameInstance(childScope));
    }

    @Before
    public void setUp() {
        childScope = scope(parentScope);
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
    public void shouldNotDefineQualifiedValue() {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Can't define symbol with qualified name 'scotch.module1.fn'");
        childScope.defineValue(qualified("scotch.module1", "fn"), t(2));
    }

    @Test
    public void shouldNotDelegateToParentWhenValueDefined() {
        childScope.defineValue(unqualified("x"), t(2));
        childScope.getValue(unqualified("x"));
        verify(parentScope, never()).getValue(unqualified("x"));
    }
}
