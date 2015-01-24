package scotch.compiler.util;

import static java.lang.System.out;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import scotch.compiler.output.GeneratedClass;

public class BytecodeClassLoader extends ClassLoader {

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
