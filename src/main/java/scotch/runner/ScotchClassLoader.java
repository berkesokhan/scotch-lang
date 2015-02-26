package scotch.runner;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import scotch.compiler.output.GeneratedClass;

public class ScotchClassLoader extends ClassLoader {

    private final Optional<File> optionalOutputPath;

    public ScotchClassLoader(Optional<File> optionalOutputPath) {
        this.optionalOutputPath = optionalOutputPath;
    }

    public Class<?> define(GeneratedClass generatedClass) {
        byte[] bytes = generatedClass.getBytes();
        optionalOutputPath.ifPresent(outputPath -> writeClass(generatedClass, bytes, outputPath));
        return defineClass(generatedClass.getClassName(), bytes, 0, bytes.length);
    }

    public List<Class<?>> defineAll(List<GeneratedClass> generatedClasses) {
        return generatedClasses.stream()
            .map(this::define)
            .collect(toList());
    }

    private void writeClass(GeneratedClass generatedClass, byte[] bytes, File outputPath) {
        File file = new File(outputPath, generatedClass.getClassName().replace('.', '/') + ".class");
        if (!file.getParentFile().mkdirs() && !file.getParentFile().exists()) {
            throw new RuntimeException("Can't define " + generatedClass.getClassName()
                + ", directory " + file.getParentFile() + " could not be created");
        }
        try (OutputStream classFile = new FileOutputStream(file)) {
            classFile.write(bytes);
            classFile.flush();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
