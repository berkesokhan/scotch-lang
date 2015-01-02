package scotch.compiler.generator;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.util.TestUtil.exec;

import org.junit.Ignore;
import org.junit.Test;
import scotch.compiler.CompileException;

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
