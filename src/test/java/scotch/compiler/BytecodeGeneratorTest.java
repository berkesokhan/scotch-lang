package scotch.compiler;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.util.TestUtil.exec;

import org.junit.Ignore;
import org.junit.Test;

public class BytecodeGeneratorTest {

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
}
