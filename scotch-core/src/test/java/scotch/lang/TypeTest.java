package scotch.lang;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static scotch.lang.Type.*;
import static scotch.lang.Unification.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class TypeTest {

    private ContextScope contexts;
    private TypeScope scope;
    private Type listType;

    @Before
    public void setUp() {
        contexts = mock(ContextScope.class);
        scope = new TypeScope(contexts);
        listType = union("List", asList(var("a")), asList(
            ctor("Node", asList(var("a")), asList(
                field("_0", var("a")),
                field("_1", lookup("List", asList(var("a"))))
            )),
            constant("Empty")
        ));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowException_whenCreatingNullaryWithLowerCaseName() {
        nullary("int");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowException_whenCreatingVariableWithUpperCaseName() {
        var("A");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowException_whenTryingToCreateUnionWithMemberHavingMoreArguments() {
        union("List", asList(var("a")), asList(
            ctor("Node", asList(var("a"), var("b")), asList(
                field("_0", var("a")),
                field("_1", lookup("List", asList(var("a"))))
            )),
            constant("Empty")
        ));
    }

    @Test
    public void shouldPropagateNullaryTypeIntoVariableType() {
        Type target = nullary("String");
        Type variable = var("a");
        shouldBeUnified(unify(target, variable), target);
        shouldBeBound(variable, target);
    }

    @Test
    public void shouldNotPropagateNullaryTypeIntoBoundVariableType() {
        Type target = nullary("String");
        Type variable = var("a");
        Type binding = nullary("Integer");
        Unification actualStatus = binding.unify(variable, scope);
        shouldBeMismatch(unify(target, variable), target, binding);
        shouldBeUnified(actualStatus, binding);
        shouldBeBound(variable, binding);
    }

    @Test
    public void shouldPropagateFunctionIntoVariable() {
        Type target = fn(nullary("Int"), nullary("String"));
        Type variable = var("a");
        shouldBeUnified(unify(target, variable), target);
        shouldBeBound(variable, target);
    }

    @Test
    public void shouldGiveCircularReference_whenUnifyingFunctionWithContainedVariable() {
        Type target = fn(nullary("Bit"), var("a"));
        Type variable = var("a");
        shouldBeCircular(unify(target, variable), target, variable);
    }

    @Test
    public void shouldUnifyVariables() {
        Type target = fn(var("a"), nullary("Int"));
        Type query = fn(var("b"), nullary("Int"));
        shouldBeUnified(unify(target, query), target);
        shouldBeBound(argumentOf(query), argumentOf(target));
    }

    @Test
    public void shouldReplaceChainsOfBoundVariables() {
        Type target = fn(var("a"), nullary("Int"));
        Type query = fn(var("b"), nullary("Int"));
        unify(target, query);
        unify(argumentOf(target), nullary("String"));
        shouldBeReplaced(target, fn(nullary("String"), nullary("Int")));
        shouldBeReplaced(query, fn(nullary("String"), nullary("Int")));
    }

    @Test
    public void shouldNotUnifyFunctionWithUnion() {
        Type function = fn(nullary("String"), nullary("Int"));
        Type union = nullary("Int");
        shouldBeMismatch(unify(function, union), function, union);
    }

    @Test
    public void shouldNotUnifyUnionWithFunction() {
        Type union = nullary("Int");
        Type function = fn(nullary("String"), nullary("Int"));
        shouldBeMismatch(unify(union, function), union, function);
    }

    @Test
    public void shouldNotUnifyBoundVariableWithFunction() {
        Type variable = var("a");
        Type function = fn(nullary("String"), nullary("Bool"));
        Type target = nullary("Int");
        unify(variable, target);
        shouldBeMismatch(unify(variable, function), target, function);
    }

    @Test
    public void shouldNotBindTypeArgument_whenConcreteTypeDoesNotMatchArgumentContext() {
        Type target = nullary("Unequal");
        Unification result = scope.bind(listType, asList(var("a", asList("Eq"))));
        Type expectedArgument = var("t1", asList("Eq"));
        when(contexts.getContext(target)).thenReturn(emptyList());
        when(contexts.getContext(expectedArgument)).thenReturn(asList("Eq"));
        shouldBeContextMismatch(scope.bind(result.getUnifiedType(), asList(target)), expectedArgument, target);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldNotBindVariable_whenVariableHasAlreadyBeenBound() {
        VariableType variable = var("a");
        Type target = nullary("Char");
        scope.bind(variable, target);
        scope.bind(variable, nullary("Int"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotGetTargetOfNonVariableType() {
        scope.getTarget(nullary("Int"));
    }

    @Test
    public void shouldPropagateContext_whenUnifyingVariableWithVariableHavingContext() {
        List<String> context = asList("Eq");
        Type variable = var("a");
        Type variableHavingContext = var("b", context);
        Type expectedResult = var("t1", context);
        Unification result = unify(variable, variableHavingContext);
        shouldBeUnified(result, expectedResult);
        shouldBeBound(variable, expectedResult);
        shouldBeBound(variableHavingContext, expectedResult);
    }

    @Test
    public void shouldPropagateContext_whenUnifyingVariableHavingContextWithVariable() {
        List<String> context = asList("Eq");
        Type variableHavingContext = var("a", context);
        Type variable = var("b");
        Type expectedResult = var("t1", context);
        Unification result = unify(variableHavingContext, variable);
        shouldBeUnified(result, expectedResult);
        shouldBeBound(variableHavingContext, expectedResult);
        shouldBeBound(variable, expectedResult);
    }

    @Test
    public void shouldMergeContext_whenUnifyingVariablesHavingContext() {
        List<String> context0 = asList("Eq", "Ord");
        List<String> context1 = asList("Typed");
        Type variable0 = var("a", context0);
        Type variable1 = var("b", context1);
        Type expectedResult = var("t1", asList("Eq", "Ord", "Typed"));
        Unification result = unify(variable0, variable1);
        shouldBeUnified(result, expectedResult);
        shouldBeBound(variable0, expectedResult);
        shouldBeBound(variable1, expectedResult);
    }

    @Test
    public void shouldNotUnifyVariableWithUnion_whenVariableContextDoesNotMatchUnionContext() {
        Type variable = var("a", asList("Typed"));
        Type union = union("Colors", asList(constant("Red"), constant("Green"), constant("Blue")));
        when(contexts.getContext(union)).thenReturn(asList("Eq", "Ord"));
        shouldBeContextMismatch(unify(variable, union), variable, union);
    }

    @Test
    public void shouldNotUnifyUnionWithVariable_whenUnionContextDoesNotMatchVariableContext() {
        Type union = union("Colors", asList(constant("Red"), constant("Green"), constant("Blue")));
        Type variable = var("a", asList("Typed"));
        when(contexts.getContext(union)).thenReturn(asList("Eq", "Ord"));
        shouldBeContextMismatch(unify(union, variable), union, variable);
    }

    @Test
    public void shouldNotUnifyVariableWithFunction_whenVariableHasContext() {
        Type variable = var("a", asList("Eq"));
        Type function = fn(nullary("Int"), nullary("String"));
        when(contexts.getContext(variable)).thenReturn(asList("Eq"));
        shouldBeContextMismatch(unify(variable, function), variable, function);
    }

    @Test
    public void shouldNotUnifyFunctionWithVariable_whenVariableHasContext() {
        Type function = fn(nullary("Int"), nullary("String"));
        Type variable = var("a", asList("Eq"));
        when(contexts.getContext(variable)).thenReturn(asList("Eq"));
        shouldBeContextMismatch(unify(function, variable), function, variable);
    }

    @Test
    public void shouldBindIntegerToUnionTypeArgument() {
        Type target = nullary("Int");
        Type expectedType = union("List", asList(target), asList(
            ctor("Node", asList(target), asList(
                field("_0", target),
                field("_1", lookup("List", asList(target)))
            )),
            constant("Empty")
        ));
        shouldBeUnified(scope.bind(listType, asList(target)), expectedType);
    }

    private void shouldBeBound(Type variable, Type target) {
        assertThat(scope.getTarget(variable), is(target));
    }

    private void shouldBeCircular(Unification result, Type target, Type variable) {
        assertFalse(result.isUnified());
        assertThat(result, is(circular(target, variable)));
    }

    private void shouldBeContextMismatch(Unification result, Type target, Type actual) {
        assertFalse(result.isUnified());
        assertThat(result, is(mismatch(target, actual, scope)));
    }

    private void shouldBeMismatch(Unification result, Type target, Type actual) {
        assertFalse(result.isUnified());
        assertThat(result, is(mismatch(target, actual)));
    }

    private void shouldBeReplaced(Type query, Type result) {
        assertThat(scope.generate(query), is(result));
    }

    private void shouldBeUnified(Unification result, Type target) {
        assertTrue(result.isUnified());
        assertThat(result, is(unified(target)));
    }

    private Unification unify(Type target, Type query) {
        return target.unify(query, scope);
    }
}
