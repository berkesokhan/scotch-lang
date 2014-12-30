package scotch.compiler.generator;

import static java.lang.System.out;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.Compiler.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.junit.Ignore;
import org.junit.Test;
import scotch.compiler.symbol.ClasspathResolver;
import scotch.runtime.Callable;

public class BytecodeGeneratorTest {

    @SuppressWarnings("unchecked")
    public static <A> A exec(String... lines) {
        try {
            ClasspathResolver resolver = new ClasspathResolver(BytecodeGenerator.class.getClassLoader());
            BytecodeClassLoader classLoader = new BytecodeClassLoader();
            classLoader.defineAll(generateBytecode(resolver, lines));
            return ((Callable<A>) classLoader.loadClass("scotch.test.ScotchModule").getMethod("run").invoke(null)).call();
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }

    private static List<GeneratedClass> generateBytecode(ClasspathResolver resolver, String... lines) {
        return compiler(resolver, "$test", lines).generateBytecode();
    }

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

    public static class BytecodeClassLoader extends ClassLoader {

        public Class<?> define(GeneratedClass generatedClass) {
            byte[] bytes = generatedClass.getBytes();
            File file = new File("build/generated-test-classes/" + generatedClass.getClassName().replace('.', '/') + ".class");
            file.getParentFile().mkdirs();
            try (OutputStream classFile = new FileOutputStream(file)) {
                classFile.write(bytes);
                classFile.flush();
                out.println("Class file written to: " + file.getAbsolutePath());
            } catch (IOException exception) {
                exception.printStackTrace(out);
            }
            return defineClass(generatedClass.getClassName(), bytes, 0, bytes.length);
        }

        public List<Class<?>> defineAll(List<GeneratedClass> generatedClasses) {
            return generatedClasses.stream()
                .map(this::define)
                .collect(toList());
        }
    }
}
