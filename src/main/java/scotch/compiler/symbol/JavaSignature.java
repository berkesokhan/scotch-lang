package scotch.compiler.symbol;

import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;

import java.lang.reflect.Method;
import java.util.Objects;
import me.qmx.jitescript.CodeBlock;

public class JavaSignature {

    public static JavaSignature fromMethod(Method method) {
        return new JavaSignature(
            p(method.getDeclaringClass()),
            method.getName(),
            sig(method.getReturnType(), method.getParameterTypes())
        );
    }

    private final String javaClass;
    private final String methodName;
    private final String signature;

    public JavaSignature(String javaClass, String methodName, String signature) {
        this.javaClass = javaClass;
        this.methodName = methodName;
        this.signature = signature;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof JavaSignature) {
            JavaSignature other = (JavaSignature) o;
            return Objects.equals(javaClass, other.javaClass)
                && Objects.equals(methodName, other.methodName)
                && Objects.equals(signature, other.signature);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(javaClass, methodName, signature);
    }

    public CodeBlock reference() {
        return new CodeBlock() {{
            invokestatic(javaClass, methodName, signature);
        }};
    }

    @Override
    public String toString() {
        return javaClass + ":" + methodName + ":" + signature;
    }
}
