package me.qmx.jitescript;

import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.H_INVOKESTATIC;
import static org.objectweb.asm.Opcodes.H_INVOKEVIRTUAL;
import static org.objectweb.asm.Type.getMethodType;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import org.objectweb.asm.Handle;

public class LambdaBlock {

    public static final Handle METAFACTORY = new Handle(
        H_INVOKESTATIC,
        p(LambdaMetafactory.class),
        "metafactory",
        sig(
            CallSite.class,
            MethodHandles.Lookup.class,
            String.class,
            MethodType.class,
            MethodType.class,
            MethodHandle.class,
            MethodType.class
        )
    );

    private final String dynamicSignature;
    private final int access;
    private String instanceType;
    private String interfaceType;
    private String signature;
    private CodeBlock body;

    public LambdaBlock(String dynamicSignature) {
        this(0, dynamicSignature);
    }

    public LambdaBlock(int access, String dynamicSignature) {
        this.dynamicSignature = dynamicSignature;
        this.access = access | ACC_SYNTHETIC | ACC_PRIVATE;
    }

    /**
     * Apply the lambda to the code label and associated class.
     */
    public void apply(JiteClass jiteClass, CodeBlock block) {
        int handleType = ((access & ACC_STATIC) == ACC_STATIC) ? H_INVOKESTATIC : H_INVOKEVIRTUAL;
        String lambdaName = jiteClass.reserveLambda();
        jiteClass.defineMethod(lambdaName, access, signature, body);
        block.invokedynamic("apply", dynamicSignature, METAFACTORY,
            getMethodType(interfaceType),
            new Handle(handleType, jiteClass.getClassName(), lambdaName, signature),
            getMethodType(instanceType == null ? interfaceType : instanceType)
        );
    }

    /**
     * Sets the signature of the lambda and the associated body.
     */
    public LambdaBlock block(String signature, CodeBlock body) {
        this.signature = signature;
        this.body = body;
        return this;
    }

    /**
     * Sets the actual interface signature.
     *
     * <p>May be the same as the {@link #interfaceType(String)} value or a specialization.</p>
     */
    public LambdaBlock instanceType(String instanceType) {
        this.instanceType = instanceType;
        return this;
    }

    /**
     * Sets the functional interface signature.
     */
    public LambdaBlock interfaceType(String interfaceType) {
        this.interfaceType = interfaceType;
        return this;
    }
}
