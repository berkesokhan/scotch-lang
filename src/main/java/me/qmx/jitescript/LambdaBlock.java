package me.qmx.jitescript;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.H_INVOKESTATIC;
import static org.objectweb.asm.Opcodes.H_INVOKEVIRTUAL;
import static org.objectweb.asm.Type.getMethodType;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import me.qmx.jitescript.util.CodegenUtils;
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
    private int          implementationAccess;
    private String       implementationSignature;
    private CodeBlock    implementationCode;
    private String       interfaceMethod;
    private String       interfaceType;
    private String       specializedSignature;
    private List<String> captureArguments;
    private String       interfaceSignature;

    /**
     * Apply the lambda to the code label and associated class.
     */
    public void apply(JiteClass jiteClass, CodeBlock block) {
        int handleType = ((implementationAccess & ACC_STATIC) == ACC_STATIC) ? H_INVOKESTATIC : H_INVOKEVIRTUAL;
        String lambdaName = jiteClass.reserveLambda();
        jiteClass.defineMethod(lambdaName, implementationAccess, implementationSignature, implementationCode);
        block.invokedynamic(interfaceMethod, getDynamicSignature(), METAFACTORY,
            getMethodType(interfaceSignature),
            new Handle(handleType, jiteClass.getClassName(), lambdaName, implementationSignature),
            getMethodType(specializedSignature == null ? interfaceSignature : specializedSignature)
        );
    }

    private String getDynamicSignature() {
        return "(" + captureArguments.stream().collect(joining()) + ")L" + interfaceType + ";";
    }

    public LambdaBlock function(String interfaceType, String interfaceMethod, String interfaceSignature) {
        this.interfaceType = interfaceType;
        this.interfaceMethod = interfaceMethod;
        this.interfaceSignature = interfaceSignature;
        return this;
    }

    public LambdaBlock specialize(String specializedSignature) {
        this.specializedSignature = specializedSignature;
        return this;
    }

    public LambdaBlock capture(Class<?>... captureArguments) {
        this.captureArguments = asList(captureArguments).stream()
            .map(CodegenUtils::ci)
            .collect(toList());
        return this;
    }

    public LambdaBlock capture(String... captureArguments) {
        this.captureArguments = new ArrayList<>(asList(captureArguments));
        return this;
    }

    public LambdaBlock delegateTo(int implementationAccess, String implementationSignature, CodeBlock implementationCode) {
        this.implementationSignature = implementationSignature;
        this.implementationAccess = implementationAccess;
        this.implementationCode = implementationCode;
        return this;
    }
}
