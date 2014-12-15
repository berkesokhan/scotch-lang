package scotch.compiler.generator;

import static me.qmx.jitescript.util.CodegenUtils.c;
import static me.qmx.jitescript.util.CodegenUtils.p;
import static me.qmx.jitescript.util.CodegenUtils.sig;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static scotch.compiler.syntax.DefinitionReference.rootRef;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import com.google.common.collect.ImmutableList;
import me.qmx.jitescript.CodeBlock;
import me.qmx.jitescript.JiteClass;
import me.qmx.jitescript.MethodDefinition;
import scotch.compiler.symbol.Type;
import scotch.compiler.symbol.Type.FunctionType;
import scotch.compiler.symbol.Type.TypeVisitor;
import scotch.compiler.syntax.Definition;
import scotch.compiler.syntax.Definition.DefinitionVisitor;
import scotch.compiler.syntax.Definition.ModuleDefinition;
import scotch.compiler.syntax.Definition.RootDefinition;
import scotch.compiler.syntax.Definition.ValueDefinition;
import scotch.compiler.syntax.DefinitionGraph;
import scotch.compiler.syntax.DefinitionReference;
import scotch.compiler.syntax.Scope;
import scotch.compiler.syntax.Value;
import scotch.compiler.syntax.Value.Apply;
import scotch.compiler.syntax.Value.BoundMethod;
import scotch.compiler.syntax.Value.IntLiteral;
import scotch.compiler.syntax.Value.ValueVisitor;
import scotch.compiler.text.SourceRange;
import scotch.runtime.Applicable;
import scotch.runtime.Callable;
import scotch.runtime.Thunk;

public class BytecodeGenerator implements DefinitionVisitor<Void>, ValueVisitor<CodeBlock> {

    private final DefinitionGraph      graph;
    private final Deque<JiteClass>     jiteClasses;
    private final List<GeneratedClass> generatedClasses;
    private final Deque<Scope>         scopes;
    private       int                  sequence;

    public BytecodeGenerator(DefinitionGraph graph) {
        this.graph = graph;
        this.jiteClasses = new ArrayDeque<>();
        this.generatedClasses = new ArrayList<>();
        this.scopes = new ArrayDeque<>();
    }

    public List<GeneratedClass> generate() {
        graph.getDefinition(rootRef()).map(definition -> definition.accept(this));
        return ImmutableList.copyOf(generatedClasses);
    }

    @Override
    public CodeBlock visit(Apply apply) {
        return new CodeBlock() {{
            String className = beginClass(Thunk.class, apply.getSourceRange());
            currentClass().defineDefaultConstructor();
            method("evaluate", ACC_PROTECTED, sig(Object.class), new CodeBlock()
                .append(generate(apply.getFunction()))
                .invokeinterface(p(Callable.class), "call", sig(Object.class))
                .checkcast(p(Applicable.class))
                .append(generate(apply.getArgument()))
                .invokeinterface(p(Applicable.class), "apply", sig(Callable.class, Callable.class))
                .areturn());
            endClass();
            newobj(className);
            dup();
            invokespecial(className, "<init>", sig(void.class));
        }};
    }

    @Override
    public CodeBlock visit(BoundMethod boundMethod) {
        return boundMethod.reference(currentScope());
    }

    @Override
    public CodeBlock visit(IntLiteral literal) {
        return new CodeBlock()
            .ldc(literal.getValue())
            .invokestatic(p(Callable.class), "box", sig(Callable.class, int.class));
    }

    @Override
    public Void visit(RootDefinition definition) {
        scoped(definition, () -> generateAll(definition.getDefinitions()));
        return null;
    }

    @Override
    public Void visit(ValueDefinition definition) {
        scoped(definition, () -> {
            String signature = definition.getType().accept(new TypeVisitor<String>() {
                @Override
                public String visit(FunctionType type) {
                    return sig(Applicable.class);
                }

                @Override
                public String visitOtherwise(Type type) {
                    return sig(Callable.class);
                }
            });
            method(definition.getMethodName(), ACC_STATIC | ACC_PUBLIC, signature, new CodeBlock() {{
                annotate(Value.class).value("memberName", definition.getSymbol().getMemberName());
                definition.markLine(this);
                append(generate(definition.getBody()));
                if (!returns()) {
                    areturn();
                }
            }});
        });
        return null;
    }

    @Override
    public Void visit(ModuleDefinition definition) {
        scoped(definition, () -> {
            beginClass(definition.getClassName(), definition.getSourceRange());
            currentClass().defineDefaultConstructor(ACC_PRIVATE);
            generateAll(definition.getDefinitions());
            endClass();
        });
        return null;
    }

    private void beginClass(String className, SourceRange sourceRange) {
        jiteClasses.push(new JiteClass(className));
        currentClass().setSourceFile(sourceRange.getSourceName());
    }

    private String beginClass(Class<?> parentClass, SourceRange sourceRange) {
        String className = currentClass().getClassName() + "$" + sequence++;
        if (parentClass.isInterface()) {
            jiteClasses.push(new JiteClass(className, p(Object.class), new String[] { p(parentClass) }));
        } else {
            jiteClasses.push(new JiteClass(className, p(parentClass), new String[0]));
        }
        currentClass().setSourceFile(sourceRange.getSourceName());
        return className;
    }

    private JiteClass currentClass() {
        return jiteClasses.peek();
    }

    private Scope currentScope() {
        return scopes.peek();
    }

    private void endClass() {
        JiteClass jiteClass = jiteClasses.pop();
        generatedClasses.add(new GeneratedClass(c(jiteClass.getClassName()), jiteClass.toBytes()));
    }

    private CodeBlock generate(Value value) {
        return value.accept(this);
    }

    private void generateAll(List<DefinitionReference> definitions) {
        definitions.stream()
            .map(this.graph::getDefinition)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(definition -> definition.accept(this));
    }

    private void method(String methodName, int modifiers, String signature, CodeBlock methodBody) {
        currentClass().addMethod(new MethodDefinition(methodName, modifiers, signature, methodBody));
    }

    private void scoped(Definition definition, Runnable runnable) {
        scopes.push(graph.getScope(definition.getReference()));
        try {
            runnable.run();
        } finally {
            scopes.pop();
        }
    }
}
