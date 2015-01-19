package scotch.compiler.syntax;

import java.util.List;
import java.util.function.Supplier;
import me.qmx.jitescript.CodeBlock;
import me.qmx.jitescript.JiteClass;
import scotch.compiler.GeneratedClass;
import scotch.compiler.symbol.Type;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.text.SourceRange;
import scotch.runtime.Callable;

public interface BytecodeGenerator {

    void addMatch(String name);

    void beginClass(String className, SourceRange sourceRange);

    void beginMatches();

    CodeBlock captureApply();

    CodeBlock captureLambda(String lambdaArgument);

    JiteClass currentClass();

    void defineDefaultConstructor(int access);

    CodeBlock enclose(Scoped scoped, Supplier<CodeBlock> supplier);

    void endClass();

    void endMatches();

    void fromRoot();

    <T extends Scoped> void generate(T scoped, Runnable runnable);

    void generateBytecode(List<DefinitionReference> references);

    Class<?>[] getCaptureAllTypes();

    List<GeneratedClass> getClasses();

    Class<?>[] getLambdaCaptureTypes();

    Class<?>[] getLambdaType();

    int getVariable(String name);

    void method(String methodName, int access, String signature, CodeBlock body);

    void releaseLambda(String lambdaArgument);

    String reserveApply();

    String reserveLambda();

    Scope scope();

    <T extends Scoped> CodeBlock scoped(T scoped, Supplier<CodeBlock> supplier);

    Class<? extends Callable> typeOf(Type type);
}
