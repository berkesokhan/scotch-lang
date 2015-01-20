package scotch.compiler.syntax;

import java.util.List;
import java.util.function.Supplier;
import me.qmx.jitescript.CodeBlock;
import me.qmx.jitescript.JiteClass;
import org.objectweb.asm.tree.LabelNode;
import scotch.compiler.GeneratedClass;
import scotch.compiler.symbol.Type;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.text.SourceRange;
import scotch.runtime.Callable;

public interface BytecodeGenerator {

    void addMatch(String name);

    LabelNode beginCase();

    void beginCases(int size);

    void beginClass(String className, SourceRange sourceRange);

    void beginMatches();

    CodeBlock captureApply();

    CodeBlock captureLambda(String lambdaArgument);

    JiteClass currentClass();

    void defineDefaultConstructor(int access);

    CodeBlock enclose(Scoped scoped, Supplier<CodeBlock> supplier);

    LabelNode endCase();

    LabelNode endCases();

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

    LabelNode nextCase();

    void releaseLambda(String lambdaArgument);

    String reserveApply();

    String reserveLambda();

    Scope scope();

    <T extends Scoped> CodeBlock scoped(T scoped, Supplier<CodeBlock> supplier);

    Class<? extends Callable> typeOf(Type type);
}
