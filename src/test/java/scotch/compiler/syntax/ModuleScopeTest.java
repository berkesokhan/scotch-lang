package scotch.compiler.syntax;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static scotch.compiler.symbol.Symbol.qualified;
import static scotch.compiler.symbol.Symbol.unqualified;
import static scotch.compiler.symbol.Type.t;
import static scotch.compiler.syntax.Scope.scope;

import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import scotch.compiler.symbol.Operator;
import scotch.compiler.symbol.SymbolResolver;

@RunWith(MockitoJUnitRunner.class)
public class ModuleScopeTest {

    @Rule public final ExpectedException exception = none();
    @Mock private SymbolResolver resolver;
    @Mock private Scope          rootScope;
    @Mock private Import         import_;
    private       Scope          moduleScope;
    private       String         moduleName;

    @Before
    public void setUp() {
        moduleName = "scotch.test";
        moduleScope = scope(rootScope, resolver, moduleName, asList(import_));
        when(import_.qualify(any(String.class), any(SymbolResolver.class))).thenReturn(Optional.empty());
    }

    @Test
    public void leavingChildScopeShouldGiveBackModuleScope() {
        assertThat(moduleScope.enterScope().leaveScope(), sameInstance(moduleScope));
    }

    @Test
    public void shouldThrow_whenEnteringScopeWithModuleName() {
        exception.expect(IllegalStateException.class);
        moduleScope.enterScope("scotch.module", emptyList());
    }

    @Test
    public void shouldQualifyDefinedValueByQualifiedSymbol_whenModuleNameOfSymbolIsSameAsScopeModuleName() {
        String memberName = "fn";
        moduleScope.defineValue(qualified(moduleName, memberName), t(2));
        assertThat(moduleScope.qualify(qualified(moduleName, memberName)), is(Optional.of(qualified(moduleName, memberName))));
    }

    @Test
    public void shouldQualifyDefinedValueByUnqualifiedSymbol() {
        String memberName = "fn";
        moduleScope.defineValue(qualified(moduleName, memberName), t(2));
        assertThat(moduleScope.qualify(unqualified(memberName)), is(Optional.of(qualified(moduleName, memberName))));
    }

    @Test
    public void shouldGetNothingWhenQualifyingSymbolNotDefinedLocallyAndNotImported() {
        assertThat(moduleScope.qualify(unqualified("fn")), is(Optional.empty()));
    }

    @Test
    public void shouldDelegateQualificationToImportWhenUnqualifiedSymbolNotDefinedLocally() {
        String externalModule = "scotch.external";
        String memberName = "fn";
        when(import_.qualify(memberName, resolver)).thenReturn(Optional.of(qualified(externalModule, memberName)));
        assertThat(moduleScope.qualify(unqualified(memberName)), is(Optional.of(qualified(externalModule, memberName))));
    }

    @Test
    public void shouldGetNothingWhenQualifyingQualifiedSymbolThatHasUnimportedModuleName() {
        assertThat(moduleScope.qualify(qualified("scotch.external", "fn")), is(Optional.empty()));
    }

    @Test
    public void shouldQualifyQualifiedSymbolThatHasImportedModuleName() {
        String moduleName = "scotch.external";
        String memberName = "fn";
        when(import_.isFrom(moduleName)).thenReturn(true);
        when(import_.qualify(memberName, resolver)).thenReturn(Optional.of(qualified(moduleName, memberName)));
        assertThat(moduleScope.qualify(qualified(moduleName, memberName)), is(Optional.of(qualified(moduleName, memberName))));
    }

    @Test
    public void shouldThrow_whenDefiningUnqualifiedOperator() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Can't define unqualified symbol 'fn'");
        moduleScope.defineOperator(unqualified("fn"), mock(Operator.class));
    }
}
