package scotch.compiler.generator;

import static java.lang.System.out;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static scotch.compiler.util.TestUtil.analyzeTypes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import scotch.compiler.symbol.ClasspathResolver;
import scotch.compiler.symbol.SymbolResolver;
import scotch.runtime.Callable;

public class BytecodeGeneratorTest {

    private SymbolResolver resolver;

    @Before
    public void setUp() {
        resolver = new ClasspathResolver(getClass().getClassLoader());
    }

    @Test
    public void shouldCompile2Plus2() throws Exception {
        BytecodeClassLoader classLoader = new BytecodeClassLoader();
        classLoader.defineAll(new BytecodeGenerator(analyzeTypes(
            resolver,
            "module test",
            "import scotch.data.num",
            "main = 2 + 2"
        )).generate());
        assertThat(((Callable) classLoader.loadClass("test.ScotchModule").getMethod("main").invoke(null)).call(), is(4));
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
