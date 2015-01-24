package scotch.compiler.syntax;

import java.util.List;
import java.util.function.Supplier;
import me.qmx.jitescript.CodeBlock;
import me.qmx.jitescript.JiteClass;
import org.objectweb.asm.tree.LabelNode;
import scotch.compiler.GeneratedClass;
import scotch.compiler.symbol.MethodSignature;
import scotch.compiler.symbol.Symbol;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.TypeInstanceDescriptor;
import scotch.compiler.syntax.reference.ClassReference;
import scotch.compiler.syntax.reference.DefinitionReference;
import scotch.compiler.syntax.reference.ModuleReference;
import scotch.compiler.syntax.scope.Scope;
import scotch.compiler.text.SourceRange;
import scotch.runtime.Callable;

public interface BytecodeGenerator {

    void addMatch(String name);

    LabelNode beginCase();

    void beginCases(int size);

    void beginClass(String className, SourceRange sourceRange);

    void beginClass(String className, String superClass, SourceRange sourceRange);

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

    void field(String fieldName, int access, String type);

    void fromRoot();

    <T extends Scoped> void generate(T scoped, Runnable runnable);

    List<GeneratedClass> generateBytecode();

    void generateBytecode(List<DefinitionReference> references);

    Class<?>[] getCaptureAllTypes();

    List<GeneratedClass> getClasses();

    default String getDataConstructorClass(Symbol symbol) {
        return scope().getDataConstructorClass(symbol);
    }

    Class<?>[] getLambdaCaptureTypes();

    Class<?>[] getLambdaType();

    default TypeInstanceDescriptor getTypeInstance(ClassReference classRef, ModuleReference moduleRef, List<Type> parameters) {
        return scope().getTypeInstance(classRef, moduleRef, parameters);
    }

    default MethodSignature getValueSignature(Symbol symbol) {
        return scope()
            .getValueSignature(symbol)
            .orElseThrow(() -> new IllegalStateException("Could not get value method for " + symbol));
    }

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
