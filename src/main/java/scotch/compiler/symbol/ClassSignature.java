package scotch.compiler.symbol;

import static me.qmx.jitescript.util.CodegenUtils.ci;
import static me.qmx.jitescript.util.CodegenUtils.p;

public abstract class ClassSignature {

    public static ClassSignature fromClass(Class<?> clazz) {
        return new JavaClassSignature(clazz);
    }

    public static ClassSignature fromSymbol(Symbol symbol) {
        return new ComputedClassSignature(symbol.getClassName());
    }

    private ClassSignature() {
        // intentionally empty
    }

    public abstract String getPath();

    public abstract String getClassId();

    private static final class ComputedClassSignature extends ClassSignature {

        private final String path;

        public ComputedClassSignature(String path) {
            this.path = path;
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public String getClassId() {
            return "L" + path + ";";
        }
    }

    private static final class JavaClassSignature extends ClassSignature {

        private final Class<?> clazz;

        public JavaClassSignature(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public String getPath() {
            return p(clazz);
        }

        @Override
        public String getClassId() {
            return ci(clazz);
        }
    }
}
