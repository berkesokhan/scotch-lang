package scotch.compiler.steps;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.data.either.Either.left;
import static scotch.data.maybe.Maybe.just;
import static scotch.data.tuple.TupleValues.tuple2;
import static scotch.data.tuple.TupleValues.tuple3;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import scotch.compiler.ClassLoaderResolver;
import scotch.compiler.Compiler;
import scotch.compiler.error.CompileException;
import scotch.compiler.util.TestUtil;
import scotch.data.either.Either.Left;
import scotch.data.maybe.Maybe;
import scotch.data.tuple.Tuple2;
import scotch.data.tuple.Tuple3;
import scotch.runtime.Callable;

public class BytecodeGeneratorTest {

    @Rule
    public final TestName testName = new TestName();

    @Test
    public void shouldCompileId() {
        String result = exec(
            "module scotch.test",
            "id = \\x -> x",
            "run = id \"Bananas!\""
        );
        assertThat(result, is("Bananas!"));
    }

    @Test
    public void shouldCompile2Plus2() {
        int result = exec(
            "module scotch.test",
            "import scotch.data.num",
            "run = 2 + 2"
        );
        assertThat(result, is(4));
    }

    @Test
    public void shouldCompileDelegated2Plus2() {
        int result = exec(
            "module scotch.test",
            "import scotch.data.num",
            "add = \\x y -> x + y",
            "run = add 2 2"
        );
        assertThat(result, is(4));
    }

    @Test
    public void shouldCompile2Plus2WithDoubles() {
        double result = exec(
            "module scotch.test",
            "import scotch.data.num",
            "add = \\x y -> x + y",
            "run = add 2.2 2.2"
        );
        assertThat(result, is(4.4));
    }

    @Test(expected = CompileException.class)
    public void shouldFailCompilation_whenThereAreErrors() {
        exec(
            "module scotch.test",
            "import scotch.data.num",
            "add = \\x y -> x + y",
            "run = add 2.2 2"
        );
    }

    @Test
    public void shouldPassNamedFunctionAsArgument() {
        int result = exec(
            "module scotch.test",
            "import scotch.data.num",
            "fn a b c d = d a b c",
            "run = fn 1 2 3 add3",
            "add3 x y z = x + y + z"
        );
        assertThat(result, is(6));
    }

    @Test
    public void shouldPassAnonymousFunctionAsArgument() {
        int result = exec(
            "module scotch.test",
            "import scotch.data.num",
            "fn a b c d = d a b c",
            "run = fn 1 2 3 (\\x y z -> x + y + z)"
        );
        assertThat(result, is(6));
    }

    @Test
    public void shouldCompileLet() {
        int result = exec(
            "module scotch.test",
            "import scotch.data.num",
            "run = let",
            "    f x = a x * 2",
            "    a g = g + g",
            "  f 2"
        );
        assertThat(result, is(8));
    }

    @Test
    public void shouldCompileConditional() {
        String result = exec(
            "module scotch.test",
            "run = if True then \"Waffles\" else \"Bananas\""
        );
        assertThat(result, is("Waffles"));
    }

    @Test
    public void shouldCompileChainedConditional() {
        int result = exec(
            "module scotch.test",
            "import scotch.data.eq",
            "import scotch.data.num",
            "",
            "run = fib 20",
            "fib = \\n -> if n == 0 then 0",
            "             else if n == 1 then 1",
            "             else fib (n - 1) + fib (n - 2)"
        );
        assertThat(result, is(6765));
    }

    @Test
    public void shouldCompileConditionalPattern() {
        int result = exec(
            "module scotch.test",
            "import scotch.data.eq",
            "import scotch.data.num",
            "",
            "run = fib 20",
            "fib 0 = 0",
            "fib 1 = 1",
            "fib n = fib (n - 1) + fib (n - 2)"
        );
        assertThat(result, is(6765));
    }

    @Test
    public void shouldCompileDataDeclaration() throws ReflectiveOperationException {
        Object result = exec(
            "module scotch.test",
            "data Maybe a = Nothing | Just a",
            "run = Just \"Waffles\""
        );
        Method getter = result.getClass().getMethod("get_0");
        assertThat(((Callable) getter.invoke(result)).call(), is("Waffles"));
    }

    @Test
    public void equivalentDataShouldBeEqual() {
        boolean result = exec(
            "module scotch.test",
            "import scotch.java",
            "data Thing a { value :: a }",
            "run = Thing 2 `javaEq?!` Thing 2"
        );
        assertThat(result, is(true));
    }

    @Test
    public void equivalentDataShouldHaveSameHashCode() {
        boolean result = exec(
            "module scotch.test",
            "import scotch.java",
            "import scotch.data.eq",
            "import scotch.data.function",
            "",
            "data Thing n { value :: n }",
            "",
            "run = (javaHash! $ Thing 2) == (javaHash! $ Thing 2)"
        );
        assertThat(result, is(true));
    }

    @Test
    public void shouldCreateDataFromInitializerWithArbitrarilyOrderedFields() {
        boolean result = exec(
            "module scotch.test",
            "import scotch.java",
            "import scotch.data.eq",
            "import scotch.data.function",
            "import scotch.data.int",
            "import scotch.data.string",
            "",
            "data QuantifiedThing a { howMany :: Int, what :: a }",
            "",
            "run = QuantifiedThing { howMany = 32, what = \"Bananas\" } `javaEq?!`",
            "      QuantifiedThing { what = \"Bananas\", howMany = 32 }"
        );
        assertThat(result, is(true));
    }

    @Ignore // TODO should not rely on runtime for correctness (implement as pattern matching mayhaps?)
    @Test
    public void shouldCopyDataWithNewFieldValues() {
        boolean result = exec(
            "module scotch.test",
            "import scotch.java",
            "import scotch.data.eq",
            "import scotch.data.function",
            "import scotch.data.int",
            "import scotch.data.string",
            "",
            "data QuantifiedThing a { howMany Int, what a }",
            "",
            "originalQuantity = QuantifiedThing { howMany = 23, what = \"Oranges\" }",
            "newQuantity = originalQuantity { howMany = 42 }",
            "run = newQuantity `javaEq?!` QuantifiedThing { howMany = 42, what = \"Oranges\" }"
        );
        assertThat(result, is(true));
    }

    @Test
    public void shouldCompileParenthesizedSignature() {
        exec(
            "module scotch.test",
            "import scotch.java",
            "import scotch.data.eq",
            "",
            "data Thing n { value :: n }",
            "",
            "($) :: (a -> b) -> a -> b",
            "right infix 0 ($)",
            "fn $ arg = fn arg",
            "",
            "run = (javaHash! $ Thing 2) == (javaHash! $ Thing 2)"
        );
    }

    @Test
    public void shouldCompileBind() {
        Left result = exec(
            "module scotch.test",
            "import scotch.control.monad",
            "import scotch.data.either",
            "run = Right \"Yes\" >>= \\which -> Left 0"
        );
        assertThat(result, is(left(0)));
    }

    @Test
    public void shouldCompileDoNotation() {
        Maybe result = exec(
            "module scotch.test",
            "",
            "import scotch.control.monad",
            "import scotch.data.function",
            "import scotch.data.maybe",
            "import scotch.data.num",
            "",
            "run = do",
            "    val <- Just 3",
            "    return $ val + 2"
        );
        assertThat(result, is(just(5)));
    }

    @Test
    public void shouldCompileTupleLiteral() {
        Tuple3<Integer, Integer, Tuple2<Integer, Integer>> tuple = exec(
            "module scotch.test",
            "import scotch.data.int",
            "",
            "run = (1, 2, (3, 4))"
        );
        assertThat(tuple, is(tuple3(1, 2, tuple2(3, 4))));
    }

    @Test
    public void listsShouldEqual() {
        boolean result = exec(
            "module scotch.test",
            "import scotch.data.eq",
            "import scotch.data.int",
            "import scotch.data.list",
            "",
            "run = [1, 2, 3] == [1, 2, 3]"
        );
        assertThat(result, is(true));
    }

    @Test
    public void emptyListsShouldEqual() {
        boolean result = exec(
            "module scotch.test",
            "import scotch.data.eq",
            "import scotch.data.list",
            "",
            "run = [] == []"
        );
        assertThat(result, is(true));
    }

    @Test
    public void listShouldEqualConsList() {
        boolean result = exec(
            "module scotch.test",
            "import scotch.data.eq",
            "import scotch.data.int",
            "import scotch.data.list",
            "",
            "run = [1, 2, 3] == 1:2:3:[]"
        );
        assertThat(result, is(true));
    }

    @Test
    public void shouldParseIgnoredPattern() {
        int result = exec(
            "module scotch.test",
            "fn = \\_ -> 2",
            "run = fn 3"
        );
        assertThat(result, is(2));
    }

    @Ignore
    @Test
    public void shouldCompileShow() {
        String result = exec(
            "module scotch.test",
            "import scotch.data.show",
            "import scotch.java",

            "instance Show Int where",
            "    show = jIntShow",

            "run = show 5"
        );
        assertThat(result, is("5"));
    }

    @Test
    public void shouldCreatePickleWithEnumConstants() {
        Object pickle = exec(
            "module scotch.test",
            "import scotch.data.num",
            "import scotch.data.int",
            "",
            "data Texture = Soft | Crunchy",
            "data Pickle { kind :: Texture, pimples :: Int }",
            "pickle = Pickle Crunchy 15",
            "run = pickle"
        );
        assertThat(pickle.toString(), is("Pickle { kind = Crunchy, pimples = 15 }"));
    }

    @Test
    public void shouldGetOrdering() {
        boolean shouldBeTruthy = exec(
            "module scotch.test",
            "import scotch.data.bool",
            "import scotch.data.eq",
            "import scotch.data.ord",
            "import scotch.data.int",
            "",
            "run = max 2 3 == 3 && max 2 3 == max 3 2",
            "   && min 2 3 == 2 && min 2 3 == min 3 2",
            "   && 2 < 3",
            "   && 3 > 2",
            "   && 2 <= 3 && 2 <= 2",
            "   && 3 >= 2 && 3 >= 3",
            "   && LessThan == compare 2 3",
            "   && GreaterThan == compare 3 2",
            "   && EqualTo == compare 2 2"
        );
        assertThat(shouldBeTruthy, is(true));
    }

    @Ignore("WIP")
    @Test
    public void shouldDestructureTuple() {
        int value = exec(
            "module scotch.test",
            "",
            "second (_, b) = b",
            "run = second (3, 2)"
        );
        assertThat(value, is(2));
    }

    @Test
    public void shouldNegateNumber() {
        int value = exec(
            "module scotch.test",
            "import scotch.data.num",
            "",
            "run = -4"
        );
        assertThat(value, is(-4));
    }

    @SuppressWarnings("unchecked")
    private <A> A exec(String... lines) {
        try {
            ClassLoaderResolver resolver = new ClassLoaderResolver(
                Optional.of(new File("build/generated-test-classes/" + testName.getMethodName())),
                Compiler.class.getClassLoader()
            );
            resolver.defineAll(TestUtil.generateBytecode(testName.getMethodName(), resolver, lines));
            return ((Callable<A>) resolver.loadClass("scotch.test.$$Module").getMethod("run").invoke(null)).call();
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }
}
