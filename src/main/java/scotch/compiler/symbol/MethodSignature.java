package scotch.compiler.symbol;

import static java.lang.reflect.Modifier.isStatic;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import me.qmx.jitescript.CodeBlock;

public abstract class MethodSignature {

    public static MethodSignature fromMethod(Method method) {
        return new JavaMethodSignature(method);
    }

    public static MethodSignature fromMethod(Class<?> clazz, String methodName) {
        try {
            return stream(clazz.getMethods())
                .filter(method -> methodName.equals(method.getName()))
                .findFirst()
                .map(MethodSignature::fromMethod)
                .orElseThrow(() -> new NoSuchMethodException("Could not find method " + methodName));
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }

    public static MethodSignature interfaceFromSymbol(Symbol symbol, List<ClassSignature> parameterTypes, ClassSignature returnType) {
        return fromSymbol(MethodType.INTERFACE, symbol, parameterTypes, returnType);
    }

    public static MethodSignature staticFromSymbol(Symbol symbol, List<ClassSignature> parameterTypes, ClassSignature returnType) {
        return fromSymbol(MethodType.STATIC, symbol, parameterTypes, returnType);
    }

    public static MethodSignature virtualFromSymbol(Symbol symbol, List<ClassSignature> parameterTypes, ClassSignature returnType) {
        return fromSymbol(MethodType.VIRTUAL, symbol, parameterTypes, returnType);
    }

    private static MethodSignature fromSymbol(MethodType methodType, Symbol symbol, List<ClassSignature> parameterTypes, ClassSignature returnType) {
        return new ScotchMethodSignature(methodType, symbol, parameterTypes, returnType);
    }

    @Override
    public boolean equals(Object o) {
        return o == this || o instanceof MethodSignature && toString().equals(o.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(toString());
    }

    public abstract CodeBlock reference();

    private interface Generator {

        CodeBlock generate(String target, String method, String signature);
    }

    private static enum MethodType {

        STATIC {
            @Override
            public CodeBlock generate(ScotchMethodSignature signature) {
                return signature.generate(new CodeBlock()::invokestatic);
            }
        },
        VIRTUAL {
            @Override
            public CodeBlock generate(ScotchMethodSignature signature) {
                return signature.generate(new CodeBlock()::invokevirtual);
            }
        },
        INTERFACE {
            @Override
            public CodeBlock generate(ScotchMethodSignature signature) {
                return signature.generate(new CodeBlock()::invokeinterface);
            }
        };

        public abstract CodeBlock generate(ScotchMethodSignature signature);
    }

    private static final class JavaMethodSignature extends MethodSignature {

        private final Method method;

        public JavaMethodSignature(Method method) {
            this.method = method;
        }

        @Override
        public CodeBlock reference() {
            if (isStatic(method.getModifiers())) {
                return generate(new CodeBlock()::invokestatic);
            } else if (method.getDeclaringClass().isInterface()) {
                return generate(new CodeBlock()::invokeinterface);
            } else if ("<init>".equals(method.getName())) {
                return generate(new CodeBlock()::invokespecial);
            } else {
                return generate(new CodeBlock()::invokevirtual);
            }
        }

        @Override
        public String toString() {
            return p(method.getDeclaringClass()) + ":" + method.getName() + ":" + sig(method.getReturnType(), method.getParameterTypes());
        }

        private CodeBlock generate(Generator generator) {
            return generator.generate(p(method.getDeclaringClass()), method.getName(), sig(method.getReturnType(), method.getParameterTypes()));
        }
    }

    private static final class ScotchMethodSignature extends MethodSignature {

        private final MethodType           methodType;
        private final Symbol               symbol;
        private final List<ClassSignature> parameterTypes;
        private final ClassSignature       returnType;

        private ScotchMethodSignature(MethodType methodType, Symbol symbol, List<ClassSignature> parameterTypes, ClassSignature returnType) {
            this.methodType = methodType;
            this.symbol = symbol;
            this.parameterTypes = parameterTypes;
            this.returnType = returnType;
        }

        @Override
        public CodeBlock reference() {
            return methodType.generate(this);
        }

        @Override
        public String toString() {
            return symbol.getClassName() + ":" + symbol.getMethodName() + ":" + getSignature();
        }

        private CodeBlock generate(Generator generator) {
            return generator.generate(symbol.getClassName(), symbol.getMethodName(), getSignature());
        }

        private String getSignature() {
            return "(" + parameterTypes.stream().map(ClassSignature::getClassId).collect(joining("")) + ")" + returnType.getClassId();
        }
    }
}
