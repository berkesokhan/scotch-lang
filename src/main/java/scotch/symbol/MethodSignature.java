package scotch.symbol;

import static java.util.Arrays.stream;
import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import me.qmx.jitescript.CodeBlock;

public class MethodSignature {

    public static MethodSignature constructor(String descriptor) {
        return fromString(MethodType.SPECIAL, descriptor);
    }

    public static MethodSignature fromConstructor(Constructor constructor) {
        return new MethodSignature(
            MethodType.SPECIAL,
            p(constructor.getDeclaringClass()),
            "<init>",
            sig(void.class, constructor.getParameterTypes())
        );
    }

    public static MethodSignature fromMethod(Method method) {
        return new MethodSignature(
            MethodType.fromAccess(method),
            p(method.getDeclaringClass()),
            method.getName(),
            sig(method.getReturnType(), method.getParameterTypes())
        );
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

    public static MethodSignature methodSignature(String descriptor) {
        return fromString(MethodType.STATIC, descriptor);
    }

    public static MethodSignature staticMethod(String className, String methodName, String signature) {
        return new MethodSignature(MethodType.STATIC, className, methodName, signature);
    }

    private static MethodSignature fromString(MethodType methodType, String descriptor) {
        String[] parts = descriptor.split(":");
        return new MethodSignature(methodType, parts[0], parts[1], parts[2]);
    }
    private final MethodType methodType;
    private final String     className;
    private final String     methodName;
    private final String     signature;

    private MethodSignature(MethodType methodType, String className, String methodName, String signature) {
        this.methodType = methodType;
        this.className = className;
        this.methodName = methodName;
        this.signature = signature;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof MethodSignature) {
            MethodSignature other = (MethodSignature) o;
            return Objects.equals(methodType, other.methodType)
                && Objects.equals(className, other.className)
                && Objects.equals(methodName, other.methodName)
                && Objects.equals(signature, other.signature);
        } else {
            return false;
        }
    }

    public String getClassName() {
        return className;
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodType, className, methodName, signature);
    }

    public CodeBlock reference() {
        return methodType.generate(this);
    }

    @Override
    public String toString() {
        return className + ":" + methodName + ":" + signature;
    }

    private enum MethodType {
        STATIC {
            @Override
            public CodeBlock generate(MethodSignature signature) {
                return new CodeBlock() {{
                    invokestatic(signature.className, signature.methodName, signature.signature);
                }};
            }
        },
        VIRTUAL {
            @Override
            public CodeBlock generate(MethodSignature signature) {
                return new CodeBlock() {{
                    invokevirtual(signature.className, signature.methodName, signature.signature);
                }};
            }
        },
        INTERFACE {
            @Override
            public CodeBlock generate(MethodSignature signature) {
                return new CodeBlock() {{
                    invokeinterface(signature.className, signature.methodName, signature.signature);
                }};
            }
        },
        SPECIAL {
            @Override
            public CodeBlock generate(MethodSignature signature) {
                return new CodeBlock() {{
                    invokespecial(signature.className, signature.methodName, signature.signature);
                }};
            }
        };

        public static MethodType fromAccess(Method method) {
            if (Modifier.isStatic(method.getModifiers())) {
                return STATIC;
            } else if (Modifier.isInterface(method.getModifiers())) {
                return INTERFACE;
            } else {
                return VIRTUAL;
            }
        }

        public abstract CodeBlock generate(MethodSignature signature);
    }
}
