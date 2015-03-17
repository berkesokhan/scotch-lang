package scotch.symbol.type;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.symbol.type.Unification.failedBinding;
import static scotch.symbol.type.Types.fn;
import static scotch.symbol.type.Types.sum;
import static scotch.symbol.type.Types.var;
import static scotch.compiler.util.TestUtil.boolType;
import static scotch.compiler.util.TestUtil.intType;

import org.junit.Test;

public class TypeTest extends UnificationTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowException_whenCreatingSumWithLowerCaseName() {
        sum("int");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowException_whenCreatingVariableWithUpperCaseName() {
        var("A");
    }

    @Test
    public void shouldPropagateSumTypeIntoVariableType() {
        Type target = sum("String");
        Type variable = var("a");
        shouldBeUnified(unify(target, variable), target);
        shouldBeBound(variable, target);
    }

    @Test
    public void shouldNotPropagateSumTypeIntoBoundVariableType() {
        Type target = sum("String");
        Type variable = var("a");
        Type binding = sum("Integer");
        Unification actualStatus = binding.unify(variable, scope);
        shouldBeMismatch(unify(target, variable), target, binding);
        shouldBeUnified(actualStatus, binding);
        shouldBeBound(variable, binding);
    }

    @Test
    public void shouldPropagateFunctionIntoVariable() {
        Type target = fn(sum("Int"), sum("String"));
        Type variable = var("a");
        shouldBeUnified(unify(target, variable), target);
        shouldBeBound(variable, target);
    }

    @Test
    public void shouldGiveCircularReference_whenUnifyingFunctionWithContainedVariable() {
        Type target = fn(sum("Bit"), var("a"));
        Type variable = var("a");
        shouldBeCircular(unify(target, variable), target, variable);
    }

    @Test
    public void shouldUnifyVariables() {
        Type target = fn(var("a"), sum("Int"));
        Type query = fn(var("b"), sum("Int"));
        shouldBeUnified(unify(target, query), target);
        shouldBeBound(argumentOf(query), argumentOf(target));
    }

    @Test
    public void shouldReplaceChainsOfBoundVariables() {
        Type target = fn(var("a"), sum("Int"));
        Type query = fn(var("b"), sum("Int"));
        unify(target, query);
        unify(argumentOf(target), sum("String"));
        shouldBeReplaced(target, fn(sum("String"), sum("Int")));
        shouldBeReplaced(query, fn(sum("String"), sum("Int")));
    }

    @Test
    public void shouldNotUnifyFunctionWithUnion() {
        Type function = fn(sum("String"), sum("Int"));
        Type union = sum("Int");
        shouldBeMismatch(unify(function, union), function, union);
    }

    @Test
    public void shouldNotUnifyUnionWithFunction() {
        Type union = sum("Int");
        Type function = fn(sum("String"), sum("Int"));
        shouldBeMismatch(unify(union, function), union, function);
    }

    @Test
    public void shouldNotUnifyBoundVariableWithFunction() {
        Type variable = var("a");
        Type function = fn(sum("String"), sum("Bool"));
        Type target = sum("Int");
        unify(variable, target);
        shouldBeMismatch(unify(variable, function), target, function);
    }

    @Test
    public void shouldNotBindVariable_whenVariableHasAlreadyBeenBound() {
        VariableType variable = var("a");
        Type target = sum("Char");
        scope.bind(variable, target);
        assertThat(scope.bind(variable, sum("Int")), is(failedBinding(sum("Int"), variable, target)));
    }

    @Test
    public void shouldMutuallyExtendContexts_whenUnifyingVariables() {
        VariableType a = var("a", asList("Eq"));
        VariableType b = var("b", asList("Show"));
        unify(b, a);
        shouldBeBound(a, var("b", asList("Eq", "Show")));
        shouldBeBound(b, var("b", asList("Eq", "Show")));
    }

    @Test
    public void shouldNotUnifySumToTargetVariable_whenSumDoesNotImplementEntireVariableContext() {
        VariableType target = var("a", asList("Eq", "Show"));
        Type sum = sum("DbCursor");
        addContext(sum, "Show");
        shouldBeContextMismatch(unify(target, sum), target, sum, asList("Eq", "Show"), asList("Show"));
    }

    @Test
    public void shouldNotUnifyFunctionToTargetVariable_whenVariableHasContext() {
        VariableType target = var("a", asList("Eq"));
        Type function = fn(var("b"), var("c"));
        shouldBeContextMismatch(unify(target, function), target, function, asList("Eq"), asList());
    }

    @Test
    public void shouldUnifySums() {
        SumType list1 = sum("List", asList(intType()));
        SumType list2 = sum("List", asList(intType()));
        shouldBeUnified(unify(list1, list2), sum("List", asList(intType())));
    }

    @Test
    public void shouldNotUnifySumsWhenArgumentsDiffer() {
        SumType list1 = sum("List", asList(intType()));
        SumType list2 = sum("List", asList(boolType()));
        shouldBeMismatch(unify(list1, list2), intType(), boolType());
    }
}
