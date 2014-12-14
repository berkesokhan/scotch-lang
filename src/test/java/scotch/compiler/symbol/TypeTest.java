package scotch.compiler.symbol;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static scotch.compiler.symbol.Type.fn;
import static scotch.compiler.symbol.Type.sum;
import static scotch.compiler.symbol.Type.var;
import static scotch.compiler.symbol.Unification.circular;
import static scotch.compiler.symbol.Unification.contextMismatch;
import static scotch.compiler.symbol.Unification.mismatch;
import static scotch.compiler.symbol.Unification.unified;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import scotch.compiler.symbol.Type.FunctionType;
import scotch.compiler.symbol.Type.TypeVisitor;
import scotch.compiler.symbol.Type.VariableType;
import scotch.compiler.syntax.DefaultTypeScope;

public class TypeTest {

    private TypeScope scope;

    @Before
    public void setUp() {
        scope = new DefaultTypeScope(new TypeGenerator());
    }

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

    @Test(expected = UnsupportedOperationException.class)
    public void shouldNotBindVariable_whenVariableHasAlreadyBeenBound() {
        VariableType variable = var("a");
        Type target = sum("Char");
        scope.bind(variable, target);
        scope.bind(variable, sum("Int"));
    }

    @Test
    public void shouldMutuallyExtendContexts_whenUnifyingVariables() {
        VariableType a = var("a", asList("Eq"));
        VariableType b = var("b", asList("Show"));
        shouldBeUnified(unify(b, a), b);
        shouldBeBound(a, var("b", asList("Eq", "Show")));
        shouldBeBound(b, var("b", asList("Eq", "Show")));
    }

    @Test
    public void shouldNotUnifySumToTargetVariable_whenSumDoesNotImplementEntireVariableContext() {
        VariableType target = var("a", asList("Eq", "Show"));
        Type sum = sum("DbCursor");
        addContext(sum, "Show");
        shouldBeContextMismatch(unify(target, sum), target, sum, asList("Eq"));
    }

    @Test
    public void shouldNotUnifyFunctionToTargetVariable_whenVariableHasContext() {
        VariableType target = var("a", asList("Eq"));
        Type function = fn(var("b"), var("c"));
        shouldBeContextMismatch(unify(target, function), target, function, asList("Eq"));
    }

    private void addContext(Type type, String... context) {
        scope.extendContext(type, asList(context).stream().map(Symbol::fromString).collect(toSet()));
    }

    private void shouldBeContextMismatch(Unification unification, Type expected, Type actual, List<String> contextDifference) {
        assertFalse(unification.isUnified());
        assertThat(unification, is(contextMismatch(expected, actual, contextDifference.stream().map(Symbol::fromString).collect(toList()))));
    }

    private Type argumentOf(Type type) {
        return type.accept(new TypeVisitor<Type>() {
            @Override
            public Type visit(FunctionType type) {
                return type.getArgument();
            }

            @Override
            public Type visitOtherwise(Type type) {
                throw new UnsupportedOperationException("Can't get argument from " + type.getClass().getSimpleName());
            }
        });
    }

    private void shouldBeBound(Type variable, Type target) {
        assertThat(scope.getTarget(variable), is(target));
    }

    private void shouldBeCircular(Unification result, Type target, Type variable) {
        assertFalse(result.isUnified());
        assertThat(result, is(circular(target, variable)));
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
