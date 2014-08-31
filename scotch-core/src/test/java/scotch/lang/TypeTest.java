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

    public static Type listOf(Type argument) {
        return union("List", asList(argument), asList(
            ctor("Node", asList(argument), asList(
                field("_0", argument),
                field("_1", lookup("List", asList(argument)))
            )),
            constant("Empty")
        ));
    }

    private final Type intType = nullary("Int");
    private final Type stringType = nullary("String");
    private final Type listType = listOf(var("a"));

    private ContextScope contextScope;
    private TypeScope typeScope;

    @Before
    public void setUp() {
        contextScope = mock(ContextScope.class);
        typeScope = new TypeScope(contextScope);
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
        Type target = stringType;
        Type variable = var("a");
        shouldBeUnified(unify(target, variable), target);
        shouldBeBound(variable, target);
    }

    @Test
    public void shouldNotPropagateNullaryTypeIntoBoundVariableType() {
        Type target = stringType;
        Type variable = var("a");
        Type binding = nullary("Integer");
        Unification actualResult = binding.unify(variable, typeScope);
        shouldBeMismatch(unify(target, variable), target, binding);
        shouldBeUnified(actualResult, binding);
        shouldBeBound(variable, binding);
    }

    @Test
    public void shouldPropagateFunctionIntoVariable() {
        Type target = fn(intType, stringType);
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
        Type target = fn(var("a"), intType);
        Type query = fn(var("b"), intType);
        shouldBeUnified(unify(target, query), target);
        shouldBeBound(argumentOf(query), argumentOf(target));
    }

    @Test
    public void shouldReplaceChainsOfBoundVariables() {
        Type target = fn(var("a"), intType);
        Type query = fn(var("b"), intType);
        unify(target, query);
        unify(argumentOf(target), stringType);
        shouldBeReplaced(target, fn(stringType, intType));
        shouldBeReplaced(query, fn(stringType, intType));
    }

    @Test
    public void shouldNotUnifyFunctionWithUnion() {
        Type function = fn(stringType, intType);
        Type union = intType;
        shouldBeMismatch(function, union);
    }

    @Test
    public void shouldNotUnifyUnionWithFunction() {
        Type union = intType;
        Type function = fn(stringType, intType);
        shouldBeMismatch(union, function);
    }

    @Test
    public void shouldNotUnifyBoundVariableWithFunction() {
        Type variable = var("a");
        Type function = fn(stringType, nullary("Bool"));
        Type target = intType;
        unify(variable, target);
        shouldBeMismatch(unify(variable, function), target, function);
    }

    @Test
    public void shouldNotBindTypeArgument_whenConcreteTypeDoesNotMatchArgumentContext() {
        Type target = nullary("Unequal");
        Unification result = typeScope.bind(listType, asList(var("a", asList("Eq"))));
        Type expectedArgument = var("t1", asList("Eq"));
        when(contextScope.getContext(target)).thenReturn(emptyList());
        when(contextScope.getContext(expectedArgument)).thenReturn(asList("Eq"));
        shouldBeContextMismatch(typeScope.bind(result.getUnifiedType(), asList(target)), expectedArgument, target);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldNotBindVariable_whenVariableHasAlreadyBeenBound() {
        VariableType variable = var("a");
        Type target = nullary("Char");
        typeScope.bind(variable, target);
        typeScope.bind(variable, intType);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotGetTargetOfNonVariableType() {
        typeScope.getTarget(intType);
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
        when(contextScope.getContext(union)).thenReturn(asList("Eq", "Ord"));
        shouldBeContextMismatch(unify(variable, union), variable, union);
    }

    @Test
    public void shouldNotUnifyUnionWithVariable_whenUnionContextDoesNotMatchVariableContext() {
        Type union = union("Colors", asList(constant("Red"), constant("Green"), constant("Blue")));
        Type variable = var("a", asList("Typed"));
        when(contextScope.getContext(union)).thenReturn(asList("Eq", "Ord"));
        shouldBeContextMismatch(unify(union, variable), union, variable);
    }

    @Test
    public void shouldNotUnifyVariableWithFunction_whenVariableHasContext() {
        Type variable = var("a", asList("Eq"));
        Type function = fn(intType, stringType);
        when(contextScope.getContext(variable)).thenReturn(asList("Eq"));
        shouldBeContextMismatch(unify(variable, function), variable, function);
    }

    @Test
    public void shouldNotUnifyFunctionWithVariable_whenVariableHasContext() {
        Type function = fn(intType, stringType);
        Type variable = var("a", asList("Eq"));
        when(contextScope.getContext(variable)).thenReturn(asList("Eq"));
        shouldBeContextMismatch(unify(function, variable), function, variable);
    }

    @Test
    public void shouldBindIntegerToUnionTypeArgument() {
        Type expectedType = listOf(intType);
        shouldBeUnified(typeScope.bind(listType, asList(intType)), expectedType);
    }

    @Test
    public void shouldUnifyLookupWithUnion_whenNamesAndArgumentsMatch() {
        Type lookup = lookup("List", asList(intType));
        Type union = listOf(intType);
        shouldBeUnified(unify(lookup, union), union);
    }

    @Test
    public void shouldNotUnifyLookupWithUnion_whenNamesDoNotMatch() {
        Type lookup = lookup("List2", asList(intType));
        Type union = listOf(intType);
        shouldBeMismatch(lookup, union);
    }

    @Test
    public void shouldNotUnifyLookupWithUnion_whenArgumentsDoNotMatch() {
        Type lookup = lookup("List", asList(stringType));
        Type union = listOf(intType);
        shouldBeMismatch(lookup, union);
    }

    @Test
    public void shouldUnifyUnionWithLookup_whenNamesAndArgumentsMatch() {
        Type union = listOf(intType);
        Type lookup = lookup("List", asList(intType));
        shouldBeUnified(unify(union, lookup), union);
    }

    @Test
    public void shouldNotUnifyUnionWithLookup_whenNamesDoNotMatch() {
        Type union = listOf(intType);
        Type lookup = lookup("List0", asList(intType));
        shouldBeMismatch(union, lookup);
    }

    @Test
    public void shouldNotUnifyUnionWithLookup_whenArgumentsDoNotMatch() {
        Type union = listOf(intType);
        Type lookup = lookup("List", asList(stringType));
        shouldBeMismatch(union, lookup);
    }

    @Test
    public void shouldPropagateLookup_whenUnifyingLookupWithVariable() {
        Type lookup = lookup("List", asList(intType));
        Type variable = var("a");
        Unification result = unify(lookup, variable);
        shouldBeUnified(result, lookup);
        shouldBeBound(variable, lookup);
    }

    @Test
    public void shouldPropagateLookup_whenUnifyingVariableWithLookup() {
        Type lookup = lookup("List", asList(intType));
        Type variable = var("a");
        Unification result = unify(variable, lookup);
        shouldBeUnified(result, lookup);
        shouldBeBound(variable, lookup);
    }

    @Test
    public void shouldNotUnifyLookupWithFunction() {
        Type lookup = lookup("List", asList(var("a")));
        Type function = fn(var("a"), var("b"));
        shouldBeMismatch(lookup, function);
    }

    @Test
    public void shouldNotUnifyFunctionWithLookup() {
        Type function = fn(var("a"), var("b"));
        Type lookup = lookup("List", asList(var("a")));
        shouldBeMismatch(function, lookup);
    }

    @Test
    public void shouldUnifyLookupWithLookup_whenNamesAndArgumentsMatch() {
        Type lookup0 = lookup("List", asList(var("a")));
        Type lookup1 = lookup("List", asList(var("a")));
        Unification result = unify(lookup0, lookup1);
        shouldBeUnified(result, lookup0);
    }

    @Test
    public void shouldNotUnifyLookupWithLookup_whenNamesDoNotMatch() {
        Type lookup0 = lookup("List", asList(var("a")));
        Type lookup1 = lookup("List?", asList(var("a")));
        shouldBeMismatch(lookup0, lookup1);
    }

    private void shouldBeBound(Type variable, Type target) {
        assertThat(typeScope.getTarget(variable), is(target));
    }

    private void shouldBeCircular(Unification result, Type target, Type variable) {
        assertFalse(result.isUnified());
        assertThat(result, is(circular(target, variable)));
    }

    private void shouldBeContextMismatch(Unification result, Type target, Type actual) {
        assertFalse(result.isUnified());
        assertThat(result, is(mismatch(target, actual, typeScope)));
    }

    private void shouldBeMismatch(Type target, Type query) {
        shouldBeMismatch(unify(target, query), target, query);
    }

    private void shouldBeMismatch(Unification result, Type target, Type actual) {
        assertFalse(result.isUnified());
        assertThat(result, is(mismatch(target, actual)));
    }

    private void shouldBeReplaced(Type query, Type result) {
        assertThat(typeScope.generate(query), is(result));
    }

    private void shouldBeUnified(Unification result, Type target) {
        assertTrue(result.isUnified());
        assertThat(result, is(unified(target)));
    }

    private Unification unify(Type target, Type query) {
        return target.unify(query, typeScope);
    }
}
